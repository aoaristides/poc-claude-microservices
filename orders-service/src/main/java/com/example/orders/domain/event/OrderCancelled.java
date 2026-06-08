package com.example.orders.domain.event;

import com.example.orders.domain.model.OrderId;

import java.time.Instant;

/** Evento: pedido cancelado (saga falhou e compensou). Particípio passado. */
public record OrderCancelled(OrderId orderId, String reason, Instant occurredAt)
        implements DomainEvent {
}
