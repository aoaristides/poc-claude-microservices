package com.example.shipping.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;

import java.util.List;
import java.util.UUID;

public interface OutboxJpaRepository extends JpaRepository<OutboxJpaEntity, UUID> {

    /**
     * Busca mensagens pendentes (não publicadas), em ordem de ocorrência.
     * PESSIMISTIC_WRITE + SKIP LOCKED permite escalar com várias instâncias.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2"))
    @Query("select o from OutboxJpaEntity o where o.publishedAt is null order by o.occurredAt asc")
    List<OutboxJpaEntity> findPending(Pageable pageable);
}
