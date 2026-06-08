package com.example.inventory.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configurações do relay do outbox, lidas de {@code inventory.outbox.*}.
 */
@ConfigurationProperties(prefix = "inventory.outbox")
public record InventoryOutboxProperties(long relayDelayMs) {
}
