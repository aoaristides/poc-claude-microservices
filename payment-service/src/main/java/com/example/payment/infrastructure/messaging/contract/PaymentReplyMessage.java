package com.example.payment.infrastructure.messaging.contract;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Contrato publicado como resposta no tópico payment.replies.v1.
 * O campo {@code reason} é omitido quando nulo (apenas em PaymentDeclined).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PaymentReplyMessage(String orderId, String reason) {

    /** Construtor para respostas sem reason (Authorized, Refunded). */
    public PaymentReplyMessage(String orderId) {
        this(orderId, null);
    }
}
