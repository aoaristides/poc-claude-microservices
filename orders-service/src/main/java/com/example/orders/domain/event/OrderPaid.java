package com.example.orders.domain.event;

import com.example.orders.domain.model.ClientId;
import com.example.orders.domain.model.OrderId;

import java.time.Instant;

/** Evento: pedido pago (saga concluída com sucesso). Particípio passado. */
public record OrderPaid(OrderId orderId, ClientId clientId, Instant occurredAt)
        implements DomainEvent {
}
