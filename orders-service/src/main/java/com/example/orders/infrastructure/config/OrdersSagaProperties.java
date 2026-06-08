package com.example.orders.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Parâmetros do timeout da saga ({@code orders.saga.*}).
 *
 * @param timeoutMs           tempo sem atualização para considerar a saga "presa"
 * @param timeoutCheckDelayMs intervalo entre varreduras do scheduler
 */
@ConfigurationProperties(prefix = "orders.saga")
public record OrdersSagaProperties(long timeoutMs, long timeoutCheckDelayMs) {
}
