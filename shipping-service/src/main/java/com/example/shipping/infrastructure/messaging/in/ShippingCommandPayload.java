package com.example.shipping.infrastructure.messaging.in;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Contrato de entrada: payload do comando {@code ScheduleDelivery} enviado pelo Orders.
 *
 * <p>{@code @JsonIgnoreProperties} garante tolerância a campos extras na evolução do schema.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
record ShippingCommandPayload(String orderId) {
}
