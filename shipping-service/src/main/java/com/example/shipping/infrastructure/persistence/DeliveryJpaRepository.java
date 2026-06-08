package com.example.shipping.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DeliveryJpaRepository extends JpaRepository<DeliveryJpaEntity, String> {
}
