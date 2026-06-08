package com.example.orders.domain.port.out;

import com.example.orders.domain.model.Order;
import com.example.orders.domain.model.OrderId;

import java.util.Optional;

/**
 * Porta de saída para persistência do aggregate Order.
 * Expõe só o que o domínio precisa — não é um CRUD genérico.
 */
public interface OrderRepository {

    Optional<Order> findById(OrderId id);

    void save(Order order);
}
