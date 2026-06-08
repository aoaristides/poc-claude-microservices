package com.example.orders.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/** Spring Data — NÃO é a porta do domínio; é detalhe de infra usado pelo adapter. */
public interface OrderJpaRepository extends JpaRepository<OrderJpaEntity, UUID> {
}
