package com.example.orders.infrastructure.messaging.contract;

/**
 * Contrato enxuto para comandos que só precisam referenciar o pedido:
 * ReleaseReservation, RefundPayment, ScheduleDelivery.
 */
public record OrderRefMessage(String orderId) {
}
