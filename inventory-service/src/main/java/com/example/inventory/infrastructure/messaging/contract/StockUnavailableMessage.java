package com.example.inventory.infrastructure.messaging.contract;

/**
 * Contrato de saída: payload da resposta StockUnavailable para o orquestrador.
 */
public record StockUnavailableMessage(String orderId, String reason) {}
