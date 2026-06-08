package com.example.inventory.application.command;

/**
 * Comando de aplicação: liberar reserva de estoque (compensação da saga).
 *
 * @param orderId identificador do pedido cuja reserva deve ser estornada
 */
public record ReleaseReservationCommand(String orderId) {}
