package com.example.orders.application.service;

import com.example.orders.domain.exception.OrderNotFoundException;
import com.example.orders.domain.port.in.GetOrderUseCase;
import com.example.orders.domain.port.out.OrderRepository;
import com.example.orders.domain.model.OrderId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application service de consulta: devolve o estado atual do pedido.
 *
 * <p>Somente leitura — usado para acompanhar o desfecho da saga (PAID/CANCELLED)
 * de fora do serviço (ex.: teste E2E). Não decide regra de negócio.
 */
@Service
public class GetOrderService implements GetOrderUseCase {

    private final OrderRepository orders;

    public GetOrderService(OrderRepository orders) {
        this.orders = orders;
    }

    @Override
    @Transactional(readOnly = true)
    public OrderView execute(OrderId orderId) {
        return orders.findById(orderId)
                .map(order -> new OrderView(order.id(), order.status()))
                .orElseThrow(() -> new OrderNotFoundException(orderId));
    }
}
