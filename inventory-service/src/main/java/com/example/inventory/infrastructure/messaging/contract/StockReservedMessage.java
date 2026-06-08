package com.example.inventory.infrastructure.messaging.contract;

/**
 * Contrato de saída: payload da resposta StockReserved para o orquestrador.
 */
public record StockReservedMessage(String orderId) {}
