package com.example.payment.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentJpaRepository extends JpaRepository<PaymentJpaEntity, String> {

    Optional<PaymentJpaEntity> findByOrderId(String orderId);
}
