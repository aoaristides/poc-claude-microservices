package com.example.orders.domain.model;

import com.example.orders.domain.exception.InvalidOrderException;
import com.example.orders.domain.exception.InvalidTransitionException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Testes de domínio puro: sem Spring, sem banco, sem mock. Rápidos e focados na regra. */
class OrderTest {

    private static OrderItem item(String sku, int qty, String price) {
        return new OrderItem(new Sku(sku), qty, Money.of(new BigDecimal(price), "BRL"));
    }

    private static ClientId aClient() {
        return new ClientId(java.util.UUID.randomUUID());
    }

    @Test
    void deve_confirmar_pedido_com_itens_e_calcular_total() {
        var order = Order.confirm(aClient(), List.of(item("SKU-1", 2, "10.00"), item("SKU-2", 1, "5.50")));

        assertThat(order.status()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(order.totalAmount()).isEqualTo(Money.of(new BigDecimal("25.50"), "BRL"));
    }

    @Test
    void deve_recusar_pedido_sem_itens() {
        assertThatThrownBy(() -> Order.confirm(aClient(), List.of()))
                .isInstanceOf(InvalidOrderException.class);
    }

    @Test
    void deve_marcar_como_pago_a_partir_de_confirmado() {
        var order = Order.confirm(aClient(), List.of(item("SKU-1", 1, "10.00")));

        var event = order.markPaid();

        assertThat(order.status()).isEqualTo(OrderStatus.PAID);
        assertThat(event.orderId()).isEqualTo(order.id());
    }

    @Test
    void nao_deve_pagar_pedido_ja_pago() {
        var order = Order.confirm(aClient(), List.of(item("SKU-1", 1, "10.00")));
        order.markPaid();

        assertThatThrownBy(order::markPaid)
                .isInstanceOf(InvalidTransitionException.class);
    }

    @Test
    void deve_cancelar_pedido_confirmado() {
        var order = Order.confirm(aClient(), List.of(item("SKU-1", 1, "10.00")));

        var event = order.cancel("estoque indisponível");

        assertThat(order.status()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(event.reason()).isEqualTo("estoque indisponível");
    }

    @Test
    void nao_deve_cancelar_pedido_pago() {
        var order = Order.confirm(aClient(), List.of(item("SKU-1", 1, "10.00")));
        order.markPaid();

        assertThatThrownBy(() -> order.cancel("qualquer"))
                .isInstanceOf(InvalidTransitionException.class);
    }
}
