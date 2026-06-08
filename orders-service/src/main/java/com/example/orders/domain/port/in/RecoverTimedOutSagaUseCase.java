package com.example.orders.domain.port.in;

import com.example.orders.domain.model.OrderId;

/** Porta de entrada: recuperar (compensar/re-drive) uma saga que estourou o timeout. */
public interface RecoverTimedOutSagaUseCase {

    void execute(OrderId orderId);
}
