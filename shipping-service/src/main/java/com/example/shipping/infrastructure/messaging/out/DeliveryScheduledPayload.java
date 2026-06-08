package com.example.shipping.infrastructure.messaging.out;

/**
 * Contrato publicado para {@code DeliveryScheduled}.
 * Compatível com o {@code SagaReplyPayload} do Orders (orderId obrigatório, reason null).
 */
record DeliveryScheduledPayload(String orderId) {
}
