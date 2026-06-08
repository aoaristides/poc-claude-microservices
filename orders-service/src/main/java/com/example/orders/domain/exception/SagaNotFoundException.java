package com.example.orders.domain.exception;

import com.example.orders.domain.model.OrderId;

/** Saga não encontrada para o pedido informado. */
public class SagaNotFoundException extends RuntimeException {
    public SagaNotFoundException(OrderId orderId) {
        super("saga não encontrada para o pedido: " + orderId.asString());
    }
}
