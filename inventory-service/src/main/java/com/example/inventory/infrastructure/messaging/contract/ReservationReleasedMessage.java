package com.example.inventory.infrastructure.messaging.contract;

/**
 * Contrato de saída: payload da resposta ReservationReleased para o orquestrador.
 */
public record ReservationReleasedMessage(String orderId) {}
