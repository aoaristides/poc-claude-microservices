package com.example.orders.domain.port.in;

import com.example.orders.domain.model.OrderId;
import com.example.orders.domain.model.OrderStatus;

/** Porta de entrada: consultar o estado atual de um pedido (somente leitura). */
public interface GetOrderUseCase {

    OrderView execute(OrderId orderId);

    /** Projeção mínima exposta para a borda — não vaza o aggregate Order. */
    record OrderView(OrderId orderId, OrderStatus status) {
    }
}
