package com.example.shipping.api;

import com.example.shipping.domain.model.DeliveryStatus;

/**
 * DTO de saída da API REST. A entidade JPA nunca é exposta diretamente.
 */
public record DeliveryResponse(
        String orderId,
        DeliveryStatus status,
        String trackingCode,
        String reason
) {
}
