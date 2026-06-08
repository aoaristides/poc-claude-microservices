#!/usr/bin/env bash
# =====================================================================
# Teste E2E da saga orquestrada com os 4 microsserviços reais via Docker.
#
# Sobe Kafka + 4 Postgres + 4 apps (docker-compose) e valida, ponta a ponta:
#   1. Caminho feliz                -> pedido PAID
#   2. Pagamento recusado (> limite)-> pedido CANCELLED (libera reserva)
#   3. Estoque insuficiente (SKU inexistente) -> pedido CANCELLED
#   4. Falha de entrega (SKU sentinela) -> pedido CANCELLED (estorna + libera)
#
# Cada cenário usa um pedido distinto e observa o desfecho por GET /orders/{id}.
#
# Variáveis de ambiente:
#   E2E_NO_BUILD=1     pula o --build (reusa imagens já construídas)
#   E2E_KEEP_STACK=1   não derruba a stack ao final (debug)
#   E2E_TIMEOUT=90     timeout (s) por cenário para atingir o estado esperado
# =====================================================================
set -euo pipefail

# ---- localização / configuração -------------------------------------
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
cd "${ROOT_DIR}"

ORDERS_URL="${ORDERS_URL:-http://localhost:8080}"
SENTINEL_SKU="SKU-FAIL-SHIP"           # deve casar com shipping.undeliverable-sku
TIMEOUT="${E2E_TIMEOUT:-90}"
COMPOSE=(docker compose -f docker-compose.yml -f docker-compose.e2e.yml)

# ---- cores / log ----------------------------------------------------
if [[ -t 1 ]]; then RED=$'\e[31m'; GREEN=$'\e[32m'; YELLOW=$'\e[33m'; BLUE=$'\e[34m'; NC=$'\e[0m'
else RED=''; GREEN=''; YELLOW=''; BLUE=''; NC=''; fi
log()  { printf '%s[e2e]%s %s\n' "${BLUE}" "${NC}" "$*"; }
ok()   { printf '%s[ ok ]%s %s\n' "${GREEN}" "${NC}" "$*"; }
fail() { printf '%s[FAIL]%s %s\n' "${RED}" "${NC}" "$*" >&2; }

FAILURES=0

# ---- dependências ---------------------------------------------------
need() { command -v "$1" >/dev/null 2>&1 || { fail "dependência ausente: $1"; exit 2; }; }
need docker
need curl
need jq

uuid() {
  if command -v uuidgen >/dev/null 2>&1; then uuidgen | tr 'A-Z' 'a-z'
  elif [[ -r /proc/sys/kernel/random/uuid ]]; then cat /proc/sys/kernel/random/uuid
  else python3 -c 'import uuid; print(uuid.uuid4())'; fi
}

# ---- ciclo de vida da stack -----------------------------------------
teardown() {
  if [[ "${E2E_KEEP_STACK:-0}" == "1" ]]; then
    log "E2E_KEEP_STACK=1 — mantendo a stack de pé."
    return
  fi
  log "derrubando a stack (down -v)…"
  "${COMPOSE[@]}" down -v --remove-orphans >/dev/null 2>&1 || true
}

on_exit() {
  local code=$?
  if [[ ${code} -ne 0 || ${FAILURES} -ne 0 ]]; then
    fail "logs recentes dos serviços (para diagnóstico):"
    "${COMPOSE[@]}" logs --tail=40 orders-service inventory-service payment-service shipping-service 2>/dev/null || true
  fi
  teardown
}
trap on_exit EXIT

build_images() {
  if [[ "${E2E_NO_BUILD:-0}" == "1" ]]; then
    log "E2E_NO_BUILD=1 — reusando imagens já construídas."
    return
  fi
  # Build SERIAL (um serviço por vez): 4 builds Maven paralelos saturam o BuildKit.
  # O cache mount do ~/.m2 (ver Dockerfile) compartilha dependências entre eles.
  local svc
  for svc in orders-service inventory-service payment-service shipping-service; do
    log "build da imagem: ${svc}…"
    "${COMPOSE[@]}" build "${svc}"
  done
  ok "imagens construídas."
}

start_stack() {
  build_images
  log "subindo a stack (Kafka + 4 Postgres + 4 apps)…"
  # --wait bloqueia até todos os healthchecks ficarem healthy (ou algum sair).
  "${COMPOSE[@]}" up -d --wait
  ok "stack de pé e saudável."
}

# ---- helpers de API -------------------------------------------------
# Dispara um checkout e devolve o orderId (stdout).
post_order() {
  local body="$1" resp http
  resp="$(curl -sS -w $'\n%{http_code}' -X POST "${ORDERS_URL}/orders" \
            -H 'Content-Type: application/json' -d "${body}")"
  http="$(tail -n1 <<<"${resp}")"
  body="$(sed '$d' <<<"${resp}")"
  if [[ "${http}" != "202" ]]; then
    fail "POST /orders retornou HTTP ${http}: ${body}"; return 1
  fi
  jq -er '.orderId' <<<"${body}"
}

# Consulta o status atual do pedido (stdout); vazio se 404/erro.
get_status() {
  local id="$1"
  curl -sS "${ORDERS_URL}/orders/${id}" | jq -r '.status // empty' 2>/dev/null || true
}

# Aguarda o pedido atingir o estado esperado dentro do timeout.
wait_for_status() {
  local id="$1" expected="$2" deadline=$(( SECONDS + TIMEOUT )) last=""
  while (( SECONDS < deadline )); do
    last="$(get_status "${id}")"
    [[ "${last}" == "${expected}" ]] && return 0
    # Estados terminais divergentes: falha rápida, sem esperar o timeout.
    if [[ "${last}" == "PAID" || "${last}" == "CANCELLED" ]] && [[ "${last}" != "${expected}" ]]; then
      fail "pedido ${id}: esperado ${expected}, mas terminou em ${last}"; return 1
    fi
    sleep 2
  done
  fail "pedido ${id}: timeout (${TIMEOUT}s) aguardando ${expected}; último status='${last:-<vazio>}'"
  return 1
}

# Executa um cenário: nome, json do pedido, status esperado.
scenario() {
  local name="$1" body="$2" expected="$3" id
  log "cenário: ${YELLOW}${name}${NC} (esperado: ${expected})"
  if ! id="$(post_order "${body}")"; then
    FAILURES=$((FAILURES+1)); return
  fi
  log "  orderId=${id}"
  if wait_for_status "${id}" "${expected}"; then
    ok "  ${name} → ${expected}"
  else
    FAILURES=$((FAILURES+1))
  fi
}

# ---- cenários -------------------------------------------------------
run_scenarios() {
  local c1 c2 c3 c4
  c1="$(uuid)"; c2="$(uuid)"; c3="$(uuid)"; c4="$(uuid)"

  # 1. Caminho feliz: SKU com estoque, valor abaixo do limite -> PAID
  scenario "caminho feliz (pedido -> PAID)" \
    "{\"clientId\":\"${c1}\",\"items\":[{\"sku\":\"SKU-1\",\"quantity\":2,\"unitPrice\":49.90,\"currency\":\"BRL\"}]}" \
    "PAID"

  # 2. Pagamento recusado: amount = 1 x 10001 > 10000 -> CANCELLED
  scenario "pagamento recusado (> limite) -> CANCELLED" \
    "{\"clientId\":\"${c2}\",\"items\":[{\"sku\":\"SKU-1\",\"quantity\":1,\"unitPrice\":10001.00,\"currency\":\"BRL\"}]}" \
    "CANCELLED"

  # 3. Estoque insuficiente: SKU inexistente -> StockUnavailable -> CANCELLED
  scenario "estoque insuficiente (SKU inexistente) -> CANCELLED" \
    "{\"clientId\":\"${c3}\",\"items\":[{\"sku\":\"SKU-INEXISTENTE\",\"quantity\":1,\"unitPrice\":10.00,\"currency\":\"BRL\"}]}" \
    "CANCELLED"

  # 4. Falha de entrega: SKU sentinela (tem estoque, valor baixo) -> shipping recusa -> CANCELLED
  scenario "falha de entrega (SKU sentinela) -> CANCELLED" \
    "{\"clientId\":\"${c4}\",\"items\":[{\"sku\":\"${SENTINEL_SKU}\",\"quantity\":1,\"unitPrice\":50.00,\"currency\":\"BRL\"}]}" \
    "CANCELLED"
}

# ---- main -----------------------------------------------------------
main() {
  start_stack
  log "aguardando estabilização dos consumers Kafka…"
  sleep 5
  run_scenarios

  echo
  if [[ ${FAILURES} -eq 0 ]]; then
    ok "TODOS os cenários passaram (4/4)."
  else
    fail "${FAILURES} cenário(s) falharam."
    exit 1
  fi
}

main "$@"
