package com.example.orders.domain.model;

import com.example.orders.domain.event.OrderCancelled;
import com.example.orders.domain.event.OrderPaid;
import com.example.orders.domain.exception.InvalidOrderException;
import com.example.orders.domain.exception.InvalidTransitionException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Aggregate root Order.
 *
 * <p>Protege suas invariantes nos próprios métodos (rico, não anêmico):
 * o estado só muda através de {@code confirm}, {@code markPaid} e {@code cancel}.
 * Não há setter público de status. A transição inválida vira exceção de domínio.
 */
public class Order {

    private final OrderId id;
    private final ClientId clientId;
    private final List<OrderItem> items;
    private OrderStatus status;

    // Construtor privado: a criação passa por factory para garantir invariantes.
    private Order(OrderId id, ClientId clientId, List<OrderItem> items, OrderStatus status) {
        this.id = id;
        this.clientId = clientId;
        this.items = new ArrayList<>(items);
        this.status = status;
    }

    /**
     * Factory de checkout: cria o pedido já CONFIRMED, pois a confirmação e o
     * início da saga acontecem na mesma ação de negócio.
     */
    public static Order confirm(ClientId clientId, List<OrderItem> items) {
        if (items == null || items.isEmpty()) {
            throw new InvalidOrderException("pedido sem itens");
        }
        return new Order(OrderId.generate(), clientId, items, OrderStatus.CONFIRMED);
    }

    /** Reconstituição a partir da persistência (usada pelo adapter, sem revalidar regra de criação). */
    public static Order rehydrate(OrderId id, ClientId clientId,
                                  List<OrderItem> items, OrderStatus status) {
        return new Order(id, clientId, items, status);
    }

    /** Marca como pago. Só é válido a partir de CONFIRMED. Retorna o evento de domínio. */
    public OrderPaid markPaid() {
        if (status != OrderStatus.CONFIRMED) {
            throw new InvalidTransitionException(
                    "só pedido CONFIRMED pode ser pago; estado atual: " + status);
        }
        this.status = OrderStatus.PAID;
        return new OrderPaid(id, clientId, Instant.now());
    }

    /** Cancela o pedido. Pedido já pago não cancela por aqui (exigiria reembolso). */
    public OrderCancelled cancel(String reason) {
        if (status == OrderStatus.PAID) {
            throw new InvalidTransitionException("pedido PAID não pode ser cancelado por esta operação");
        }
        this.status = OrderStatus.CANCELLED;
        return new OrderCancelled(id, reason, Instant.now());
    }

    /** Soma dos subtotais. A factory garante itens não vazios. */
    public Money totalAmount() {
        return items.stream()
                .map(OrderItem::subtotal)
                .reduce(Money::add)
                .orElseThrow(() -> new InvalidOrderException("pedido sem itens"));
    }

    public OrderId id() {
        return id;
    }

    public ClientId clientId() {
        return clientId;
    }

    public OrderStatus status() {
        return status;
    }

    public List<OrderItem> items() {
        return List.copyOf(items);
    }
}
