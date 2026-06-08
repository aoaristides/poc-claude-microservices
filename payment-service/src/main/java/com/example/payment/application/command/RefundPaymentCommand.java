package com.example.payment.application.command;

/**
 * Comando de estorno de pagamento.
 * Traduzido do contrato Kafka pelo adapter de entrada (ACL).
 */
public record RefundPaymentCommand(String orderId) {
}
