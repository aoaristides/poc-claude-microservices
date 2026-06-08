package com.example.orders.domain.saga;

import com.example.orders.domain.model.OrderId;

import java.util.Objects;

/**
 * Resposta de um participante, já traduzida para a linguagem do domínio (ACL na borda).
 * O {@code reason} é preenchido apenas em respostas de falha.
 */
public record SagaReply(OrderId orderId, SagaReplyType type, String reason) {

    public SagaReply {
        Objects.requireNonNull(orderId, "orderId");
        Objects.requireNonNull(type, "type");
    }

    public static SagaReply of(OrderId orderId, SagaReplyType type) {
        return new SagaReply(orderId, type, null);
    }

    public static SagaReply withReason(OrderId orderId, SagaReplyType type, String reason) {
        return new SagaReply(orderId, type, reason);
    }
}
