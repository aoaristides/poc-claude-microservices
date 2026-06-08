package com.example.shipping.infrastructure.messaging.out;

/**
 * Contrato publicado para {@code DeliveryFailed}.
 * Compatível com o {@code SagaReplyPayload} do Orders (orderId + reason).
 */
record DeliveryFailedPayload(String orderId, String reason) {
}
