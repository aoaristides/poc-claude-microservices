package com.example.shipping.infrastructure.persistence;

import com.example.shipping.domain.model.Delivery;
import com.example.shipping.domain.port.out.DeliveryRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;

/**
 * Adapter JPA da porta {@link DeliveryRepository}.
 *
 * <p>Traduz entre o aggregate de domínio e a entidade JPA.
 * O domínio nunca conhece {@link DeliveryJpaEntity}.
 */
@Component
class DeliveryRepositoryAdapter implements DeliveryRepository {

    private final DeliveryJpaRepository jpa;

    DeliveryRepositoryAdapter(DeliveryJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public void save(Delivery delivery) {
        jpa.save(toEntity(delivery));
    }

    @Override
    public Optional<Delivery> findByOrderId(String orderId) {
        return jpa.findById(orderId).map(this::toDomain);
    }

    private DeliveryJpaEntity toEntity(Delivery d) {
        return new DeliveryJpaEntity(
                d.getOrderId(),
                d.getStatus(),
                d.getTrackingCode(),
                d.getReason(),
                Instant.now());
    }

    private Delivery toDomain(DeliveryJpaEntity e) {
        return Delivery.reconstitute(
                e.getOrderId(),
                e.getStatus(),
                e.getTrackingCode(),
                e.getReason());
    }
}
