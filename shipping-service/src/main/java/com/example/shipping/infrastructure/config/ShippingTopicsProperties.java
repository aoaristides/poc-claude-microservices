package com.example.shipping.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Nomes dos tópicos do shipping, ligados a {@code shipping.topics.*} no application.yml.
 * Centralizar evita string mágica espalhada pelos adapters.
 */
@ConfigurationProperties(prefix = "shipping.topics")
public record ShippingTopicsProperties(
        String commands,
        String replies
) {
}
