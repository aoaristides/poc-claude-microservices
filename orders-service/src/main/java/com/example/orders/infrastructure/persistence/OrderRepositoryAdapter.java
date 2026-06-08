package com.example.orders.infrastructure.persistence;

import com.example.orders.domain.model.ClientId;
import com.example.orders.domain.model.Money;
import com.example.orders.domain.model.Order;
import com.example.orders.domain.model.OrderId;
import com.example.orders.domain.model.OrderItem;
import com.example.orders.domain.model.Sku;
import com.example.orders.domain.port.out.OrderRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Adapter de persistência do Order. Traduz explicitamente domínio &lt;-&gt; JPA.
 * Sim, dá trabalho; é o ponto da hexagonal: os dois modelos não se acoplam.
 */
@Component
class OrderRepositoryAdapter implements OrderRepository {

    private final OrderJpaRepository repo;

    OrderRepositoryAdapter(OrderJpaRepository repo) {
        this.repo = repo;
    }

    @Override
    public Optional<Order> findById(OrderId id) {
        return repo.findById(id.value()).map(this::toDomain);
    }

    @Override
    public void save(Order order) {
        // Update: carrega a entidade gerenciada e ajusta só o que muda no ciclo (status).
        // Mantém o @Version coerente sem expor versão no domínio.
        var existing = repo.findById(order.id().value()).orElse(null);
        if (existing != null) {
            existing.setStatus(order.status());
            repo.save(existing);
            return;
        }
        // Insert: novo pedido.
        var total = order.totalAmount();
        var items = order.items().stream()
                .map(i -> new OrderItemEmbeddable(
                        i.sku().value(),
                        i.quantity(),
                        i.unitPrice().amount(),
                        i.unitPrice().currency().getCurrencyCode()))
                .toList();
        var entity = new OrderJpaEntity(
                order.id().value(),
                order.clientId().value(),
                order.status(),
                total.amount(),
                total.currency().getCurrencyCode(),
                Instant.now(),
                items);
        repo.save(entity);
    }

    private Order toDomain(OrderJpaEntity e) {
        List<OrderItem> items = e.getItems().stream()
                .map(i -> new OrderItem(
                        new Sku(i.getSku()),
                        i.getQuantity(),
                        Money.of(i.getUnitPrice(), i.getCurrency())))
                .toList();
        return Order.rehydrate(
                new OrderId(e.getId()),
                new ClientId(e.getClientId()),
                items,
                e.getStatus());
    }
}
