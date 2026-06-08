package com.example.orders.application.service;

import com.example.orders.domain.event.DomainEvent;
import com.example.orders.domain.event.OrderPaid;
import com.example.orders.domain.exception.SagaNotFoundException;
import com.example.orders.domain.model.ClientId;
import com.example.orders.domain.model.Money;
import com.example.orders.domain.model.Order;
import com.example.orders.domain.model.OrderItem;
import com.example.orders.domain.model.OrderStatus;
import com.example.orders.domain.model.Sku;
import com.example.orders.domain.port.out.OrderRepository;
import com.example.orders.domain.port.out.OutboxPort;
import com.example.orders.domain.port.out.SagaRepository;
import com.example.orders.domain.saga.CheckoutSaga;
import com.example.orders.domain.saga.SagaCommand;
import com.example.orders.domain.saga.SagaReply;
import com.example.orders.domain.saga.SagaReplyType;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProcessSagaReplyServiceTest {

    private final SagaRepository sagas = mock(SagaRepository.class);
    private final OrderRepository orders = mock(OrderRepository.class);
    private final OutboxPort outbox = mock(OutboxPort.class);
    // dispatcher real sobre mocks de porta: testa orquestração ponta a ponta na camada de aplicação
    private final SagaCommandDispatcher dispatcher = new SagaCommandDispatcher(orders, outbox);
    private final ProcessSagaReplyService service = new ProcessSagaReplyService(sagas, dispatcher);

    private static CheckoutSaga newSaga() {
        return CheckoutSaga.start(
                com.example.orders.domain.model.OrderId.generate(),
                Money.of(new BigDecimal("100.00"), "BRL"));
    }

    private static Order confirmedOrder() {
        return Order.confirm(
                new ClientId(UUID.randomUUID()),
                List.of(new OrderItem(new Sku("SKU-1"), 1, Money.of(new BigDecimal("100.00"), "BRL"))));
    }

    @Test
    void deve_falhar_quando_saga_nao_existe() {
        when(sagas.findByOrderId(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(
                SagaReply.of(com.example.orders.domain.model.OrderId.generate(), SagaReplyType.STOCK_RESERVED)))
                .isInstanceOf(SagaNotFoundException.class);
    }

    @Test
    void estoque_reservado_registra_comando_de_autorizacao_de_pagamento() {
        var saga = newSaga();
        when(sagas.findByOrderId(any())).thenReturn(Optional.of(saga));

        service.execute(SagaReply.of(saga.orderId(), SagaReplyType.STOCK_RESERVED));

        var captor = ArgumentCaptor.forClass(SagaCommand.class);
        verify(outbox).register(captor.capture());
        assertThat(captor.getValue().type()).isEqualTo(SagaCommand.Type.AUTHORIZE_PAYMENT);
    }

    @Test
    void entrega_agendada_marca_pedido_como_pago_e_publica_evento() {
        var saga = newSaga();
        saga.onStockReserved();
        saga.onPaymentAuthorized();   // agora em AWAITING_DELIVERY
        when(sagas.findByOrderId(any())).thenReturn(Optional.of(saga));

        var order = confirmedOrder();
        when(orders.findById(any())).thenReturn(Optional.of(order));

        service.execute(SagaReply.of(saga.orderId(), SagaReplyType.DELIVERY_SCHEDULED));

        // o pedido foi marcado como pago...
        assertThat(order.status()).isEqualTo(OrderStatus.PAID);
        verify(orders).save(order);

        // ...e o evento OrderPaid foi para o outbox
        var captor = ArgumentCaptor.forClass(DomainEvent.class);
        verify(outbox).register(captor.capture());
        assertThat(captor.getValue()).isInstanceOf(OrderPaid.class);
    }
}
