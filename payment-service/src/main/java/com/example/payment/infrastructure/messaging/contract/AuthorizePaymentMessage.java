package com.example.payment.infrastructure.messaging.contract;

import java.math.BigDecimal;

/**
 * Contrato recebido do comando AuthorizePayment.
 * Espelha o {@code AuthorizePaymentMessage} do orders-service.
 */
public record AuthorizePaymentMessage(String orderId, BigDecimal amount, String currency) {
}
