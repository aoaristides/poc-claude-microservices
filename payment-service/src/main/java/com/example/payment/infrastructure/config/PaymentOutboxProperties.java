package com.example.payment.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Configuração do relay de outbox. */
@ConfigurationProperties(prefix = "payment.outbox")
public record PaymentOutboxProperties(long relayDelayMs) {
}
