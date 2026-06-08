package com.example.shipping.infrastructure.messaging.in;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Contrato de entrada: payload do comando {@code ScheduleDelivery} enviado pelo Orders.
 *
 * <p>{@code items} chega para que o shipping decida a entregabilidade (ex.: SKU não
 * atendido pela transportadora). {@code @JsonIgnoreProperties} garante tolerância a
 * campos extras e a mensagens antigas (sem {@code items}) na evolução do schema.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
record ShippingCommandPayload(String orderId, List<Item> items) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Item(String sku, int quantity) {
    }
}
