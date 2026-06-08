package com.example.orders.domain.exception;

import com.example.orders.domain.model.OrderId;

/** Pedido não encontrado para o id informado. */
public class OrderNotFoundException extends RuntimeException {
    public OrderNotFoundException(OrderId id) {
        super("pedido não encontrado: " + id.asString());
    }
}
