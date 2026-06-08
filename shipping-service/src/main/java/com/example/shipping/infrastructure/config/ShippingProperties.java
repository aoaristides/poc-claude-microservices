package com.example.shipping.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propriedades gerais do shipping, ligadas a {@code shipping.*} no application.yml.
 *
 * <p>{@code simulateFailure}: quando {@code true}, o serviço responde {@code DeliveryFailed}
 * para exercitar a compensação da saga (estorno de pagamento + liberação de reserva).
 */
@ConfigurationProperties(prefix = "shipping")
public record ShippingProperties(
        boolean simulateFailure
) {
}
