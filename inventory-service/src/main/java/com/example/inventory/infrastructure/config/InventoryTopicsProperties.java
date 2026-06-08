package com.example.inventory.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Nomes dos tópicos do inventory-service, lidos de {@code inventory.topics.*} no application.yml.
 * Centralizar evita string mágica espalhada pelos adapters.
 */
@ConfigurationProperties(prefix = "inventory.topics")
public record InventoryTopicsProperties(
        String inventoryCommands,
        String inventoryReplies
) {
}
