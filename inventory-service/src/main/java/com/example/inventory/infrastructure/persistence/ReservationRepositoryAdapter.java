package com.example.inventory.infrastructure.persistence;

import com.example.inventory.domain.model.Reservation;
import com.example.inventory.domain.model.ReservationStatus;
import com.example.inventory.domain.model.ReservedItem;
import com.example.inventory.domain.port.out.ReservationRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Adapter de persistência para Reservation.
 * Traduz entre o modelo de domínio e as entidades JPA.
 */
@Component
public class ReservationRepositoryAdapter implements ReservationRepository {

    private final ReservationJpaRepository jpa;

    public ReservationRepositoryAdapter(ReservationJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<Reservation> findByOrderId(String orderId) {
        return jpa.findById(orderId).map(this::toDomain);
    }

    @Override
    public void save(Reservation reservation) {
        var entity = jpa.findById(reservation.getOrderId())
                .orElseGet(() -> new ReservationJpaEntity(
                        reservation.getOrderId(),
                        reservation.getStatus().name(),
                        reservation.getCreatedAt()));

        entity.setStatus(reservation.getStatus().name());

        List<ReservationItemJpaEntity> itemEntities = reservation.getItems().stream()
                .map(item -> new ReservationItemJpaEntity(
                        reservation.getOrderId(), item.sku(), item.quantity()))
                .toList();
        entity.setItems(itemEntities);

        jpa.save(entity);
    }

    private Reservation toDomain(ReservationJpaEntity entity) {
        List<ReservedItem> items = entity.getItems().stream()
                .map(i -> new ReservedItem(i.getSku(), i.getQuantity()))
                .toList();
        return new Reservation(
                entity.getOrderId(),
                ReservationStatus.valueOf(entity.getStatus()),
                items,
                entity.getCreatedAt());
    }
}
