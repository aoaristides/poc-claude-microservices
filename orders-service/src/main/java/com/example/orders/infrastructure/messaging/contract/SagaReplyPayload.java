package com.example.orders.infrastructure.messaging.contract;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Payload comum das respostas dos participantes. {@code reason} só vem em falhas.
 * {@code @JsonIgnoreProperties} dá tolerância a campos extras (evolução de schema).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SagaReplyPayload(String orderId, String reason) {
}
