package com.example.shipping.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propriedades gerais do shipping, ligadas a {@code shipping.*} no application.yml.
 *
 * <p>{@code simulateFailure}: quando {@code true}, o serviço responde {@code DeliveryFailed}
 * de forma global (DEV) para exercitar a compensação da saga.
 * <p>{@code undeliverableSku}: SKU sentinela que dispara {@code DeliveryFailed} apenas para
 * o pedido que o contém — gatilho determinístico e per-request usado no teste E2E.
 */
@ConfigurationProperties(prefix = "shipping")
public record ShippingProperties(
        boolean simulateFailure,
        String undeliverableSku
) {
}
