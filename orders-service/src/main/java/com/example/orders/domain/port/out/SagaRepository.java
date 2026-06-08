package com.example.orders.domain.port.out;

import com.example.orders.domain.model.OrderId;
import com.example.orders.domain.saga.CheckoutSaga;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** Porta de saída para persistência do estado da saga. */
public interface SagaRepository {

    Optional<CheckoutSaga> findByOrderId(OrderId orderId);

    void save(CheckoutSaga saga);

    /**
     * Ids de pedidos cujas sagas estão "presas": não-terminais e sem atualização
     * desde {@code updatedBefore}. Base do mecanismo de timeout/recuperação.
     */
    List<OrderId> findStaleOrderIds(Instant updatedBefore);
}
