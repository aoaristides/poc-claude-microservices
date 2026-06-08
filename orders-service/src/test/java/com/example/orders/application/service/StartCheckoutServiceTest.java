package com.example.orders.application.service;

import com.example.orders.application.command.StartCheckoutCommand;
import com.example.orders.domain.model.ClientId;
import com.example.orders.domain.model.Money;
import com.example.orders.domain.model.Order;
import com.example.orders.domain.model.OrderItem;
import com.example.orders.domain.model.Sku;
import com.example.orders.domain.port.out.OrderRepository;
import com.example.orders.domain.port.out.OutboxPort;
import com.example.orders.domain.port.out.SagaRepository;
import com.example.orders.domain.saga.SagaCommand;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/** Testa a orquestração do use case com stubs das portas — sem Spring, sem infra. */
class StartCheckoutServiceTest {

    private final OrderRepository orders = mock(OrderRepository.class);
    private final SagaRepository sagas = mock(SagaRepository.class);
    private final OutboxPort outbox = mock(OutboxPort.class);
    private final StartCheckoutService service = new StartCheckoutService(orders, sagas, outbox);

    @Test
    void inicia_checkout_persistindo_pedido_saga_e_comando_de_reserva() {
        var command = new StartCheckoutCommand(
                new ClientId(UUID.randomUUID()),
                List.of(new OrderItem(new Sku("SKU-1"), 2, Money.of(new BigDecimal("10.00"), "BRL"))));

        var orderId = service.execute(command);

        assertThat(orderId).isNotNull();
        verify(orders).save(org.mockito.ArgumentMatchers.any(Order.class));
        verify(sagas).save(org.mockito.ArgumentMatchers.any());

        // primeiro comando da saga deve ser RESERVE_STOCK com o item correto
        var captor = ArgumentCaptor.forClass(SagaCommand.class);
        verify(outbox).register(captor.capture());
        var registered = captor.getValue();
        assertThat(registered.type()).isEqualTo(SagaCommand.Type.RESERVE_STOCK);
        assertThat(registered.items()).hasSize(1);
        assertThat(registered.items().get(0).sku()).isEqualTo(new Sku("SKU-1"));
    }
}
