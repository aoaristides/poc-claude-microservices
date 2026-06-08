package com.example.orders.domain.saga;

/**
 * Tipos de resposta que os participantes (Inventory, Payment, Shipping) devolvem
 * à saga. Cada um dispara uma transição na máquina de estados.
 */
public enum SagaReplyType {
    STOCK_RESERVED,
    STOCK_UNAVAILABLE,
    PAYMENT_AUTHORIZED,
    PAYMENT_DECLINED,
    PAYMENT_REFUNDED,
    DELIVERY_SCHEDULED,
    DELIVERY_FAILED,
    RESERVATION_RELEASED
}
