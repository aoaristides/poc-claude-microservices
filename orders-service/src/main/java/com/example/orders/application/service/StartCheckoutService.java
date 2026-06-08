package com.example.orders.application.service;

import com.example.orders.application.command.StartCheckoutCommand;
import com.example.orders.domain.model.Order;
import com.example.orders.domain.model.OrderId;
import com.example.orders.domain.port.in.StartCheckoutUseCase;
import com.example.orders.domain.port.out.OrderRepository;
import com.example.orders.domain.port.out.OutboxPort;
import com.example.orders.domain.port.out.SagaRepository;
import com.example.orders.domain.saga.CheckoutSaga;
import com.example.orders.domain.saga.ReservationItem;
import com.example.orders.domain.saga.SagaCommand;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Application service: inicia o checkout.
 *
 * <p>Order + Saga + Outbox commitam na MESMA transação local. Não há I/O externo
 * aqui (nada de chamar Kafka direto): o primeiro comando da saga (ReserveStock)
 * vai para o outbox e é publicado depois pelo relay. É o que garante atomicidade.
 */
@Service
public class StartCheckoutService implements StartCheckoutUseCase {

    private final OrderRepository orders;
    private final SagaRepository sagas;
    private final OutboxPort outbox;

    public StartCheckoutService(OrderRepository orders, SagaRepository sagas, OutboxPort outbox) {
        this.orders = orders;
        this.sagas = sagas;
        this.outbox = outbox;
    }

    @Override
    @Transactional
    public OrderId execute(StartCheckoutCommand command) {
        // 1. Domínio cria o pedido confirmado (valida invariantes na factory)
        var order = Order.confirm(command.clientId(), command.items());

        // 2. Inicia a saga carregando o total (necessário para autorizar o pagamento depois)
        var saga = CheckoutSaga.start(order.id(), order.totalAmount());

        orders.save(order);
        sagas.save(saga);

        // 3. Primeiro passo: reservar estoque. Vai para o outbox, não direto para o Kafka.
        var reservationItems = toReservationItems(order);
        outbox.register(SagaCommand.reserveStock(order.id(), reservationItems));

        return order.id();
    }

    private List<ReservationItem> toReservationItems(Order order) {
        return order.items().stream()
                .map(item -> new ReservationItem(item.sku(), item.quantity()))
                .toList();
    }
}
