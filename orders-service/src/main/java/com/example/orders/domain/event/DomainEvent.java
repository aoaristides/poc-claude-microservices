package com.example.orders.domain.event;

import java.time.Instant;

/**
 * Marcador de evento de domínio. Fato passado, imutável, sem dependência de infra.
 * Sealed para deixar explícito o conjunto fechado de eventos do contexto.
 */
public sealed interface DomainEvent permits OrderPaid, OrderCancelled {
    Instant occurredAt();
}
