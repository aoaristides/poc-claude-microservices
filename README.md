# POC — Saga Orquestrada de Pedidos (Java 21 + Spring Boot 3.5 + Kafka)

Sistema de pedidos distribuído demonstrando **saga orquestrada** com arquitetura
**hexagonal + DDD**, **Kafka** (JSON), **PostgreSQL** (database-per-service), Outbox,
Inbox (idempotência) e compensação. Código em inglês; comentários e docs em PT-BR.

## Serviços

| Serviço | Papel | App | DB | Consome | Publica |
|---|---|---|---|---|---|
| **orders-service** | Orquestrador da saga | 8080 | 5432 | `*.replies.v1` | `*.commands.v1`, `orders.events.v1` |
| **inventory-service** | Reserva de estoque | 8081 | 5433 | `inventory.commands.v1` | `inventory.replies.v1` |
| **payment-service** | Cobrança | 8082 | 5434 | `payment.commands.v1` | `payment.replies.v1` |
| **shipping-service** | Agendamento de entrega | 8083 | 5435 | `shipping.commands.v1` | `shipping.replies.v1` |

Cada serviço tem seu próprio README com detalhes. O orquestrador é o coração da saga:
ver [orders-service/README.md](orders-service/README.md).

## Fluxo da saga (checkout)

```
POST /orders (orders-service)
  └─ ReserveStock ───────────────► inventory ─ StockReserved ─┐
  ◄───────────────────────────────────────────────────────────┘
  └─ AuthorizePayment ───────────► payment   ─ PaymentAuthorized ─┐
  ◄──────────────────────────────────────────────────────────────┘
  └─ ScheduleDelivery ───────────► shipping  ─ DeliveryScheduled ─┐
  ◄──────────────────────────────────────────────────────────────┘
  └─ Pedido PAID  (orders.events.v1: OrderPaid)
```

Em falha, o orquestrador **compensa na ordem inversa** (estorno de pagamento, liberação
de reserva) e cancela o pedido. Há ainda **timeout de saga** para participantes mudos.

## Contratos (compatibilidade entre serviços)

- Mensagens: value JSON; headers `messageId`, `eventType`, `schemaVersion`; key = `orderId`.
- Comandos (orders → participante): `ReserveStock`, `ReleaseReservation`, `AuthorizePayment`,
  `RefundPayment`, `ScheduleDelivery`.
- Respostas (participante → orders): `StockReserved`/`StockUnavailable`/`ReservationReleased`,
  `PaymentAuthorized`/`PaymentDeclined`/`PaymentRefunded`, `DeliveryScheduled`/`DeliveryFailed`.
- Idempotência em todo consumer (tabela `inbox_message`) + Outbox em todo produtor.

## Como rodar o sistema completo

```bash
# 1. Sobe Kafka + 4 Postgres
docker compose up -d

# 2. Em 4 terminais (cada serviço cria seu schema via Flyway e os tópicos no startup):
(cd orders-service    && mvn spring-boot:run)
(cd inventory-service && mvn spring-boot:run)
(cd payment-service   && mvn spring-boot:run)
(cd shipping-service  && mvn spring-boot:run)

# 3. Dispara um checkout (caminho feliz: SKU-1 tem estoque; valor < limite de R$10.000)
curl -i -X POST http://localhost:8080/orders \
  -H 'Content-Type: application/json' \
  -d '{
        "clientId": "11111111-1111-1111-1111-111111111111",
        "items": [ {"sku": "SKU-1", "quantity": 2, "unitPrice": 49.90, "currency": "BRL"} ]
      }'
```

### Exercitando compensações
- **Pagamento recusado**: envie `unitPrice * quantity > 10000` → `payment` declina → libera reserva → pedido CANCELLED.
- **Falha na entrega**: suba o `shipping-service` com `--shipping.simulate-failure=true` → estorna pagamento + libera reserva → CANCELLED.
- **Estoque insuficiente**: use um `sku` inexistente ou `quantity` enorme → `inventory` responde `StockUnavailable`.

## Testes

```bash
# Por serviço (rápido, sem Docker):
(cd orders-service && mvn test)        # 21 testes (domínio + aplicação)
(cd inventory-service && mvn test)     # 19 testes
(cd payment-service && mvn test)       # 19 testes
(cd shipping-service && mvn test)      # 14 testes

# Integração end-to-end do orquestrador (Postgres + Kafka reais via Testcontainers):
(cd orders-service && mvn verify)      # + 2 testes de integração
```

> **Nota Docker (Testcontainers):** se aparecer `client version 1.32 is too old`, é o
> docker-java embarcado usando API antiga. O `orders-service/pom.xml` já fixa
> `-Dapi.version=1.44` no failsafe. Ver [orders-service/README.md](orders-service/README.md).

## Decisões e limites (declarados)

- **JSON no Kafka** (decisão do projeto). Evolução para Avro/Protobuf + Schema Registry
  fica documentada, fora de escopo.
- Fatores de replicação Kafka = 1 e seed de estoque alto: **apenas DEV**.
- `payment` usa gateway simulado determinístico (aprova `amount <= 10000` em BRL).
- `shipping` agenda com sucesso por padrão; `shipping.simulate-failure=true` força falha
  para exercitar a compensação.
- Cada serviço tem idempotência de consumer (inbox) **e** de negócio (por `orderId`).
