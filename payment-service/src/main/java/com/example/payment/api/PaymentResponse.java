package com.example.payment.api;

import com.example.payment.domain.model.Payment;

import java.math.BigDecimal;

/** DTO de resposta da API REST de debug. Entidade de domínio não sai do service. */
public record PaymentResponse(
        String orderId,
        BigDecimal amount,
        String currency,
        String status,
        String reason
) {
    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(
                payment.getOrderId(),
                payment.getAmount().amount(),
                payment.getAmount().currency().getCurrencyCode(),
                payment.getStatus().name(),
                payment.getReason());
    }
}
