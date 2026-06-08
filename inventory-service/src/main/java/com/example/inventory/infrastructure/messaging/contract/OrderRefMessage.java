package com.example.inventory.infrastructure.messaging.contract;

/**
 * Contrato de entrada: payload de ReleaseReservation (apenas referência ao pedido).
 */
public record OrderRefMessage(String orderId) {}
