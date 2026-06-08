package com.example.orders.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Nomes dos tópicos da saga, ligados a {@code orders.topics.*} no application.yml.
 * Centralizar evita string mágica espalhada pelos adapters.
 */
@ConfigurationProperties(prefix = "orders.topics")
public record OrdersTopicsProperties(
        String inventoryCommands,
        String paymentCommands,
        String shippingCommands,
        String orderEvents,
        String inventoryReplies,
        String paymentReplies,
        String shippingReplies
) {
}
