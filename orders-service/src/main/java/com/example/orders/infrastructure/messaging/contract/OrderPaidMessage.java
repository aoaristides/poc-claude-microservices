package com.example.orders.infrastructure.messaging.contract;

import java.time.Instant;

/** Contrato publicado do evento OrderPaid (integração ampla via orders.events). */
public record OrderPaidMessage(String orderId, String clientId, Instant occurredAt) {
}
