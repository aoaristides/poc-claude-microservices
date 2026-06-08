package com.example.payment.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

/**
 * Configuração do gateway simulado de autorização.
 * Ligada a {@code payment.authorization.*} no application.yml.
 */
@ConfigurationProperties(prefix = "payment.authorization")
public record PaymentAuthorizationProperties(BigDecimal maxAmount) {
}
