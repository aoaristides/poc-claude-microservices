package com.example.payment.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Nomes dos tópicos do payment-service, ligados a {@code payment.topics.*} no application.yml.
 * Centraliza strings de tópico para evitar magic strings espalhadas.
 */
@ConfigurationProperties(prefix = "payment.topics")
public record PaymentTopicsProperties(
        String paymentCommands,
        String paymentReplies
) {
}
