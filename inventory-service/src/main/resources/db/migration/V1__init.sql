-- =====================================================================
-- Schema inicial do Inventory Service.
-- Tabelas: stock_item, reservation, reservation_item, outbox e inbox.
-- Nomes em inglês para casar com o código.
-- =====================================================================

-- Estoque por SKU (aggregate StockItem)
CREATE TABLE stock_item (
    sku                VARCHAR(64)  PRIMARY KEY,
    available_quantity INT          NOT NULL CHECK (available_quantity >= 0),
    version            BIGINT       NOT NULL DEFAULT 0    -- lock otimista (@Version)
);

-- Reserva de pedido (aggregate Reservation)
CREATE TABLE reservation (
    order_id   VARCHAR(64)  PRIMARY KEY,
    status     VARCHAR(20)  NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- Itens da reserva (chave composta order_id + sku)
CREATE TABLE reservation_item (
    order_id VARCHAR(64)  NOT NULL REFERENCES reservation(order_id),
    sku      VARCHAR(64)  NOT NULL,
    quantity INT          NOT NULL,
    PRIMARY KEY (order_id, sku)
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
