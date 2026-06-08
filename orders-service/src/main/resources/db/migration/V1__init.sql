-- =====================================================================
-- Schema inicial do Orders Service.
-- Tabelas: aggregate Order, estado da saga, outbox e inbox.
-- Nomes em inglês para casar com o código.
-- =====================================================================

-- Aggregate root: Order
CREATE TABLE orders (
    id           UUID PRIMARY KEY,
    client_id    UUID         NOT NULL,
    status       VARCHAR(20)  NOT NULL,
    total_amount NUMERIC(15,2) NOT NULL,
    total_currency VARCHAR(3)  NOT NULL,
    version      BIGINT       NOT NULL DEFAULT 0,   -- lock otimista (@Version)
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- Itens do pedido (entidades internas do aggregate)
CREATE TABLE order_item (
    order_id     UUID         NOT NULL REFERENCES orders(id),
    sku          VARCHAR(64)  NOT NULL,
    quantity     INT          NOT NULL,
    unit_price   NUMERIC(15,2) NOT NULL,
    currency     VARCHAR(3)   NOT NULL,
    PRIMARY KEY (order_id, sku)
);

-- Estado da saga de checkout (persistência obrigatória)
CREATE TABLE checkout_saga (
    id             UUID PRIMARY KEY,
    order_id       UUID         NOT NULL REFERENCES orders(id),
    state          VARCHAR(30)  NOT NULL,
    total_amount   NUMERIC(15,2) NOT NULL,
    total_currency VARCHAR(3)   NOT NULL,
    failure_reason TEXT,
    version        BIGINT       NOT NULL DEFAULT 0,
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_checkout_saga_order UNIQUE (order_id)
);

-- Passos já concluídos = o que precisa ser compensado em caso de falha
CREATE TABLE saga_step (
    saga_id      UUID         NOT NULL REFERENCES checkout_saga(id),
    step         VARCHAR(40)  NOT NULL,
    PRIMARY KEY (saga_id, step)
);

-- OUTBOX: garante atomicidade entre mudança de estado e publicação no Kafka
CREATE TABLE outbox (
    id             UUID PRIMARY KEY,
    aggregate_type VARCHAR(50)  NOT NULL,
    aggregate_id   VARCHAR(64)  NOT NULL,
    topic          VARCHAR(120) NOT NULL,
    msg_key        VARCHAR(64)  NOT NULL,   -- = orderId, preserva ordem por partição
    event_type     VARCHAR(80)  NOT NULL,
    payload        TEXT         NOT NULL,   -- JSON do contrato publicado
    occurred_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    published_at   TIMESTAMPTZ
);
-- Índice parcial: o relay só varre o que ainda não foi publicado
CREATE INDEX idx_outbox_pending ON outbox (occurred_at) WHERE published_at IS NULL;

-- INBOX: deduplicação de mensagens recebidas (idempotência de consumer)
CREATE TABLE inbox_message (
    message_id   VARCHAR(64)  PRIMARY KEY,
    processed_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);
