package com.example.orders.infrastructure.persistence;

import com.example.orders.domain.saga.SagaState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SagaJpaRepository extends JpaRepository<SagaJpaEntity, UUID> {

    Optional<SagaJpaEntity> findByOrderId(UUID orderId);

    /** Sagas não-terminais (estado fora de {@code terminal}) paradas desde {@code before}. */
    @Query("select s.orderId from SagaJpaEntity s "
            + "where s.state not in :terminal and s.updatedAt < :before")
    List<UUID> findStaleOrderIds(@Param("terminal") Collection<SagaState> terminal,
                                 @Param("before") Instant before);
}
