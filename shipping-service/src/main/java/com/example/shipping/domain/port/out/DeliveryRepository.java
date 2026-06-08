package com.example.shipping.domain.port.out;

import com.example.shipping.domain.model.Delivery;

import java.util.Optional;

/** Porta de saída: persistência do aggregate {@link Delivery}. */
public interface DeliveryRepository {

    void save(Delivery delivery);

    Optional<Delivery> findByOrderId(String orderId);
}
