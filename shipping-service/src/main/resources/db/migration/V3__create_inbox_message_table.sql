-- Tabela de deduplicação de consumer (inbox).
-- message_id = header messageId do Kafka (UUID string).
-- processed_at registrado para auditoria e possível TTL futuro.
create table inbox_message (
    message_id   varchar(36)  not null,
    processed_at timestamptz  not null,
    constraint pk_inbox_message primary key (message_id)
);
