-- =====================================================================
-- Schema inicial do Payment Service.
-- Tabelas: aggregate Payment, outbox e inbox.
-- Nomes em inglês para casar com o código.
-- =====================================================================

-- Aggregate root: Payment (PK = orderId, um pagamento por pedido)
CREATE TABLE payment (
    order_id   VARCHAR(64)   PRIMARY KEY,
    amount     NUMERIC(15,2) NOT NULL,
    currency   VARCHAR(3)    NOT NULL,
    status     VARCHAR(20)   NOT NULL,      -- AUTHORIZED | DECLINED | REFUNDED
    reason     TEXT,                        -- preenchido apenas em DECLINED
    version    BIGINT        NOT NULL DEFAULT 0,  -- lock otimista (@Version)
    created_at TIMESTAMPTZ   NOT NULL DEFAULT now()
);

-- OUTBOX: garante atomicidade entre mudança de estado e publicação no Kafka
CREATE TABLE outbox (
    id             UUID         PRIMARY KEY,
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
