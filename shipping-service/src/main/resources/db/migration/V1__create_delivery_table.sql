-- Tabela do aggregate Delivery.
-- order_id é PK (um pedido → uma entrega).
-- version para otimistic locking (@Version do JPA).
create table delivery (
    order_id     varchar(36)  not null,
    status       varchar(20)  not null,
    tracking_code varchar(50),
    reason       varchar(500),
    version      bigint       not null default 0,
    created_at   timestamptz  not null,
    constraint pk_delivery primary key (order_id)
);
