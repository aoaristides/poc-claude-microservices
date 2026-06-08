package com.example.orders.infrastructure.messaging.contract;

import java.time.Instant;

/** Contrato publicado do evento OrderCancelled. */
public record OrderCancelledMessage(String orderId, String reason, Instant occurredAt) {
}
