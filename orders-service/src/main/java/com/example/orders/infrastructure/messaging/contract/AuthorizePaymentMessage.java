package com.example.orders.infrastructure.messaging.contract;

import java.math.BigDecimal;

/** Contrato publicado do comando AuthorizePayment. */
public record AuthorizePaymentMessage(String orderId, BigDecimal amount, String currency) {
}
