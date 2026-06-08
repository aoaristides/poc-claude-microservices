package com.example.payment.infrastructure.messaging.contract;

/**
 * Contrato recebido do comando RefundPayment.
 * Espelha o {@code OrderRefMessage} do orders-service.
 */
public record RefundPaymentMessage(String orderId) {
}
