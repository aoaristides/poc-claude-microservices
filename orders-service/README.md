# Orders Service — Saga Orquestrada (Hexagonal + DDD + Kafka)

Serviço de Pedidos que **orquestra** a saga de checkout: confirma o pedido, reserva
estoque, autoriza pagamento e agenda entrega — com **compensação** automática em caso
de falha. Código em inglês; comentários e docs em PT-BR.

## Stack

- Java 21, Spring Boot 3.5
- PostgreSQL (database-per-service)
- Apache Kafka (comandos e respostas da saga)
- Flyway (schema), JPA/Hibernate (persistência)

## Arquitetura (Hexagonal)

```
domain/          núcleo puro — Order, CheckoutSaga, eventos, portas. SEM Spring/JPA/Kafka.
application/     use cases — abrem transação, orquestram o domínio.
infrastructure/  adapters — JPA, Kafka (outbox/inbox), config.
api/             entrada HTTP (REST) + tradução de erro (RFC 7807).
```

A seta de dependência aponta **para dentro**: `api/infrastructure → application → domain`.

## Padrões aplicados

| Padrão | Onde | Por quê |
|---|---|---|
| **Saga orquestrada** | `CheckoutSaga` (domínio) + `ProcessSagaReplyService` | Fluxo crítico, auditável; compensação na ordem inversa |
| **Saga timeout** | `CheckoutSaga.onTimeout` + `SagaTimeoutService` + `SagaTimeoutScheduler` | Saga presa (participante mudo) é recuperada/compensada, não fica pendurada |
| **Outbox** | `OutboxAdapter` + `OutboxRelay` | Atomicidade entre mudança de estado e publicação no Kafka |
| **Inbox (dedup)** | `InboxStore` + `SagaReplyListener` | Idempotência de consumer (Kafka é at-least-once) |
| **ACL** | `SagaReplyMapper`, contratos em `messaging/contract` | Domínio não conhece o schema publicado dos participantes |
| **DLT + retry** | `KafkaConfig` | Mensagem venenosa não bloqueia nem entra em loop |

## Timeout da saga

Um scheduler (`SagaTimeoutScheduler`) varre periodicamente sagas presas — não-terminais
e sem atualização desde `orders.saga.timeout-ms` — e dispara a recuperação. A decisão é do
domínio (`CheckoutSaga.onTimeout`): para estados de espera, compensa na ordem inversa
(no `AWAITING_STOCK` libera a reserva de forma defensiva); para estados de compensação,
re-emite o comando idempotente até o participante confirmar. Cada saga é recuperada em
sua própria transação. Config em `application.yml` → `orders.saga.*`.

> [trade-off] Sem consulta de status ao participante, há risco de compensar um passo que
> teve sucesso mas cuja resposta atrasou. Em produção: status-check ou contagem de tentativas
> antes de compensar.

## Como rodar

```bash
# 1. Sobe Postgres + Kafka
docker compose up -d

# 2. Roda o serviço (Flyway cria o schema; tópicos criados no startup)
mvn spring-boot:run

# 3. Inicia um checkout
curl -i -X POST http://localhost:8080/orders \
  -H 'Content-Type: application/json' \
  -d '{
        "clientId": "11111111-1111-1111-1111-111111111111",
        "items": [
          {"sku": "SKU-1", "quantity": 2, "unitPrice": 49.90, "currency": "BRL"}
        ]
      }'
# -> 202 Accepted, body {"orderId":"..."}; a saga publica ReserveStock e segue o fluxo.
```

## Testes

```bash
mvn test      # 21 testes de domínio + aplicação, rápidos e sem Docker
mvn verify    # acima + testes de integração (Testcontainers): sobe Postgres + Kafka reais
```

- **Domínio** (`OrderTest`, `CheckoutSagaTest`): regra pura, sem Spring — inclui caminho
  feliz, todas as compensações, barreira de transição inválida e os caminhos de timeout.
- **Aplicação** (`*ServiceTest`): orquestração com stubs de porta (Mockito).
- **Integração** (`CheckoutSagaIntegrationIT`, sufixo `IT` → roda no `verify` via failsafe):
  fluxo ponta a ponta com Postgres + Kafka reais — outbox → relay → Kafka → listener →
  dedup → avanço da saga; cobre caminho feliz, idempotência de resposta duplicada e timeout.

### Nota sobre Docker (Testcontainers)

Docker Desktop recente removeu o suporte à API < 1.40. O docker-java embarcado no
Testcontainers usa 1.32 por padrão e **só respeita a system property `api.version`**
(a env `DOCKER_API_VERSION` é ignorada). Por isso o `pom.xml` fixa `-Dapi.version=1.44`
no JVM do failsafe. Sem isso, o erro é `client version 1.32 is too old`.

## Trade-offs e limites conhecidos (declarados)

- **Saga via Kafka command/reply** é mais complexa que REST síncrono; ganha
  desacoplamento temporal e auditoria. Justificável pelo fluxo crítico.
- **Outbox por polling** (`@Scheduled`, ~500ms): latência baixa porém não-zero.
  Em volume maior, migrar para CDC (Debezium).
- **`OutboxRelay` envia ao Kafka dentro da transação** que segura o lock das linhas
  (SKIP LOCKED). Aceitável para o exemplo; sob alta carga, considere publicar fora
  da transação e confirmar com callback.
- **JSON nos contratos** (decisão do projeto). Mantido de propósito; o caminho de evolução
  para Avro/Protobuf + Schema Registry fica documentado mas fora de escopo.
- Participantes (Inventory, Payment, Shipping) **não fazem parte** deste serviço;
  os tópicos de resposta são consumidos como se eles existissem.
