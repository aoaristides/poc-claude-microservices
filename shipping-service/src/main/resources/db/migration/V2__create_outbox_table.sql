-- Tabela do Outbox: mensagens a publicar no Kafka.
-- id = messageId usado para dedup no consumer (orders-service inbox).
-- published_at nulo = pendente; preenchido = publicado pelo relay.
create table outbox (
    id             uuid         not null,
    aggregate_type varchar(50)  not null,
    aggregate_id   varchar(36)  not null,
    topic          varchar(200) not null,
    msg_key        varchar(36)  not null,
    event_type     varchar(100) not null,
    payload        text         not null,
    occurred_at    timestamptz  not null,
    published_at   timestamptz,
    constraint pk_outbox primary key (id)
);

create index idx_outbox_pending on outbox (occurred_at) where published_at is null;
