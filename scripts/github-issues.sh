#!/usr/bin/env bash
#
# Cria as issues do projeto no GitHub, separadas por serviço.
# - Tarefas já implementadas: cria e FECHA (reason: completed).
# - Melhorias / pendências: cria e deixa ABERTA.
#
# Pré-requisito: `gh auth login` já executado (autenticado em github.com).
# Uso: bash scripts/github-issues.sh
#
set -euo pipefail

REPO="aoaristides/poc-claude-microservices"

# ----------------------------------------------------------------------------
# Labels (idempotente: --force atualiza se já existir)
# ----------------------------------------------------------------------------
label() { gh label create "$1" --color "$2" --description "$3" --force >/dev/null 2>&1 || true; }

label "orders"      "1f6feb" "orders-service (orquestrador da saga)"
label "inventory"   "0e8a16" "inventory-service (reserva de estoque)"
label "payment"     "b60205" "payment-service (cobrança)"
label "shipping"    "5319e7" "shipping-service (entrega)"
label "infra"       "5a5a5a" "infraestrutura / cross-cutting"
label "enhancement" "a2eeef" "melhoria / nova capacidade"
label "tech-debt"   "fbca04" "dívida técnica / hardening"
label "testing"     "bfd4f2" "testes / qualidade"

# ----------------------------------------------------------------------------
# Helpers
# ----------------------------------------------------------------------------
done_issue() { # $1=title  $2=body  $3=labels(csv)
  local url
  url=$(gh issue create --repo "$REPO" --title "$1" --body "$2" --label "$3")
  gh issue close "$url" --reason completed >/dev/null
  echo "  [done] $url"
}

open_issue() { # $1=title  $2=body  $3=labels(csv)
  local url
  url=$(gh issue create --repo "$REPO" --title "$1" --body "$2" --label "$3")
  echo "  [open] $url"
}

# ============================================================================
# IMPLEMENTADAS  (criadas e fechadas)
# ============================================================================

echo "== orders-service (implementadas) =="
done_issue "orders: domínio Order (aggregate + value objects + regras)" \
  "Aggregate \`Order\` rico (confirm/markPaid/cancel), VOs \`Money\`, \`OrderId\`, \`ClientId\`, \`Sku\`, \`OrderItem\` e exceções de domínio. Sem dependência de framework." \
  "orders"
done_issue "orders: saga orquestrada (CheckoutSaga: estados, transições, compensações)" \
  "Aggregate \`CheckoutSaga\` como máquina de estados; transições do caminho feliz e compensações na ordem inversa; barreira de idempotência via \`require(state)\`." \
  "orders"
done_issue "orders: mensageria (Outbox+Relay, Inbox, listener+ACL, DLT)" \
  "Outbox + OutboxRelay (publicação atômica), Inbox (dedup), SagaReplyListener + SagaReplyMapper (ACL), DefaultErrorHandler + DLT." \
  "orders"
done_issue "orders: timeout da saga (scheduler + onTimeout + compensação defensiva)" \
  "Scheduler varre sagas presas; \`CheckoutSaga.onTimeout\` decide compensação/re-drive; recuperação por saga em transação própria." \
  "orders"
done_issue "orders: persistência Postgres via Flyway + adapters JPA" \
  "Schema (orders, order_item, checkout_saga, saga_step, outbox, inbox_message); adapters traduzindo domínio <-> JPA." \
  "orders"
done_issue "orders: API REST de checkout (RFC 7807)" \
  "POST /orders inicia o checkout (202 Accepted); DTOs de borda; GlobalExceptionHandler com ProblemDetail." \
  "orders"
done_issue "orders: testes de domínio + aplicação" \
  "21 testes (Order, CheckoutSaga incl. timeout, services com mocks de porta)." \
  "orders,testing"
done_issue "orders: testes de integração com Testcontainers (Postgres + Kafka)" \
  "CheckoutSagaIntegrationIT: fluxo ponta a ponta (outbox->relay->Kafka->listener->dedup), idempotência e timeout, via failsafe (mvn verify)." \
  "orders,testing"

echo "== inventory-service (implementadas) =="
done_issue "inventory: domínio (StockItem, Reservation) + hexagonal/DDD" \
  "Aggregates \`StockItem\` e \`Reservation\` com invariantes nos métodos; estrutura hexagonal." \
  "inventory"
done_issue "inventory: reserva all-or-nothing (StockReservationService)" \
  "Domain service reserva todos os itens atomicamente; não decrementa nada se algum item faltar -> StockUnavailable." \
  "inventory"
done_issue "inventory: idempotência de negócio (reserva/liberação por orderId)" \
  "Reserva repetida não decrementa de novo; ReleaseReservation em reserva inexistente/liberada é no-op." \
  "inventory"
done_issue "inventory: mensageria (listener+ACL, Outbox+Relay, Inbox, DLT)" \
  "Consome inventory.commands.v1, publica inventory.replies.v1; dedup via inbox; error handler + DLT." \
  "inventory"
done_issue "inventory: persistência Flyway + seed de estoque" \
  "Tabelas stock_item/reservation/reservation_item/outbox/inbox_message; seed SKU-1..SKU-5 (1000 un)." \
  "inventory"
done_issue "inventory: testes de domínio + aplicação" \
  "19 testes (StockItem, StockReservationService all-or-nothing, services com mocks)." \
  "inventory,testing"

echo "== payment-service (implementadas) =="
done_issue "payment: domínio (Payment aggregate + Money VO) + hexagonal/DDD" \
  "Aggregate \`Payment\` (AUTHORIZED/DECLINED/REFUNDED) com transições ricas; VO \`Money\`." \
  "payment"
done_issue "payment: política de autorização (gateway simulado determinístico)" \
  "\`PaymentAuthorizationPolicy\`: aprova amount<=limite (config) em BRL; senão recusa com reason. Regra no domínio." \
  "payment"
done_issue "payment: refund + idempotência de negócio (por orderId)" \
  "AuthorizePayment repetido re-publica o resultado anterior; refund só de AUTHORIZED, idempotente." \
  "payment"
done_issue "payment: mensageria (listener+ACL, Outbox+Relay, Inbox, DLT)" \
  "Consome payment.commands.v1, publica payment.replies.v1; dedup via inbox; error handler + DLT." \
  "payment"
done_issue "payment: persistência Postgres via Flyway" \
  "Tabela payment (order_id PK, amount, currency, status, reason, version) + outbox + inbox_message." \
  "payment"
done_issue "payment: testes de domínio + aplicação" \
  "19 testes (política de autorização, transições, services com mocks)." \
  "payment,testing"

echo "== shipping-service (implementadas) =="
done_issue "shipping: domínio (Delivery aggregate) + hexagonal/DDD" \
  "Aggregate \`Delivery\` (SCHEDULED/FAILED) com trackingCode determinístico." \
  "shipping"
done_issue "shipping: política de agendamento (falha simulável)" \
  "\`DeliverySchedulingPolicy\`: sucesso por padrão; flag shipping.simulate-failure=true força DeliveryFailed para exercitar compensação." \
  "shipping"
done_issue "shipping: idempotência de negócio (por orderId)" \
  "ScheduleDelivery repetido re-publica o resultado anterior sem reprocessar." \
  "shipping"
done_issue "shipping: mensageria (listener+ACL, Outbox+Relay, Inbox, DLT)" \
  "Consome shipping.commands.v1, publica shipping.replies.v1; dedup via inbox; error handler + DLT." \
  "shipping"
done_issue "shipping: persistência Postgres via Flyway" \
  "Tabela delivery (order_id PK, status, tracking_code, reason, version) + outbox + inbox_message." \
  "shipping"
done_issue "shipping: testes de domínio + aplicação" \
  "14 testes (schedule/trackingCode determinístico, política de falha, services com mocks)." \
  "shipping,testing"

echo "== infra / cross-cutting (implementadas) =="
done_issue "infra: docker-compose unificado (Kafka KRaft + 4 Postgres)" \
  "1 Kafka + 1 Postgres por serviço (database-per-service), portas 5432-5435." \
  "infra"
done_issue "infra: README do sistema + tabela de contratos" \
  "Visão geral, fluxo da saga, contratos (headers/eventTypes/payloads), como rodar." \
  "infra"
done_issue "infra: repositório git + publicação no GitHub" \
  "git init, .gitignore raiz, commit inicial e push (repo público)." \
  "infra"

# ============================================================================
# MELHORIAS / PENDÊNCIAS  (criadas e deixadas ABERTAS)
# ============================================================================

echo "== melhorias cross-cutting (abertas) =="
open_issue "CI no GitHub Actions (build + testes dos 4 serviços)" \
  "Workflow matrix por serviço (mvn verify). Rascunho já em .github/workflows/ci.yml, pendente de commit/merge." \
  "infra,enhancement"
open_issue "Teste E2E da saga (4 serviços juntos via docker-compose)" \
  "Subir tudo e validar caminho feliz (pedido -> PAID) e compensações (pagamento recusado, falha de entrega, estoque insuficiente)." \
  "infra,testing"
open_issue "Migrar JSON -> Avro/Protobuf + Schema Registry" \
  "Substituir JSON por schema versionado com compatibilidade (backward) e Schema Registry. Decisão atual é JSON (escopo)." \
  "infra,enhancement"
open_issue "Observabilidade: OpenTelemetry tracing + Micrometer + propagação de traceId" \
  "Tracing distribuído ponta a ponta, métricas (lag de consumer, DLT) e correlação via header traceId nas mensagens." \
  "infra,enhancement"
open_issue "Outbox via CDC (Debezium) no lugar do polling" \
  "Reduzir latência do relay e carga de polling lendo o WAL do Postgres." \
  "infra,enhancement,tech-debt"
open_issue "Outbox relay: publicar fora da transação que segura o lock" \
  "Hoje o relay envia ao Kafka dentro da tx que mantém o lock (SKIP LOCKED). Reduzir contenção sob carga." \
  "infra,tech-debt"
open_issue "Config de produção do Kafka (replication 3, min.insync.replicas 2, partições)" \
  "Hoje replication=1 e poucas partições (DEV). Dimensionar para produção." \
  "infra,tech-debt"
open_issue "Containerizar os apps (Dockerfile) + compose/k8s completos" \
  "Imagens dos 4 serviços e orquestração completa (compose com apps ou manifests k8s/Helm)." \
  "infra,enhancement"
open_issue "Spring Security + autenticação nos endpoints REST" \
  "Proteger os endpoints HTTP (atualmente abertos)." \
  "infra,enhancement"
open_issue "DLQ/DLT: alerta e processo de reprocessamento" \
  "Alertar quando há mensagem em *.DLT e definir reprocessamento manual/automático." \
  "infra,enhancement"
open_issue "Contract testing dos schemas dos tópicos" \
  "Testes de contrato entre orders e participantes para evitar quebra de payload." \
  "infra,testing"

echo "== melhorias por serviço (abertas) =="
open_issue "orders: timeout com status-check no participante antes de compensar" \
  "Hoje o timeout compensa 'cego'. Consultar status do participante (ou contar tentativas) antes de compensar evita compensar passo bem-sucedido com resposta atrasada." \
  "orders,enhancement"
open_issue "orders: CQRS / read model para consulta de pedidos do cliente" \
  "Projeção otimizada para listar pedidos por cliente, separada do modelo de escrita." \
  "orders,enhancement"
open_issue "orders: Idempotency-Key no POST /orders" \
  "Evitar checkout duplicado em retry do cliente HTTP." \
  "orders,enhancement"
open_issue "inventory: testes de integração com Testcontainers" \
  "Cobrir adapters JPA e consumo/publicação Kafka com Postgres/Kafka reais." \
  "inventory,testing"
open_issue "payment: testes de integração com Testcontainers" \
  "Cobrir adapters JPA e consumo/publicação Kafka com Postgres/Kafka reais." \
  "payment,testing"
open_issue "payment: abstrair gateway real (porta) + Resilience4j (retry/circuit breaker)" \
  "Trocar o gateway simulado por porta para provedor real, com timeout/retry/circuit breaker." \
  "payment,enhancement"
open_issue "shipping: testes de integração com Testcontainers" \
  "Cobrir adapters JPA e consumo/publicação Kafka com Postgres/Kafka reais." \
  "shipping,testing"
open_issue "shipping: regra de agendamento real (janela/CEP/transportadora)" \
  "Substituir a política simulada por regra de negócio real de agendamento." \
  "shipping,enhancement"

echo ""
echo "Concluído. Resumo:"
gh issue list --repo "$REPO" --state all --limit 100 | wc -l | xargs echo "  total de issues:"
