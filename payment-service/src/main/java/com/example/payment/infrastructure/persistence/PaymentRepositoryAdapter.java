package com.example.payment.infrastructure.persistence;

import com.example.payment.domain.model.Money;
import com.example.payment.domain.model.Payment;
import com.example.payment.domain.model.PaymentStatus;
import com.example.payment.domain.port.out.PaymentRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;

/**
 * Adapter de persistência do aggregate Payment.
 *
 * <p>Isola a entidade JPA do domínio. Converte entre Payment (domínio) e
 * PaymentJpaEntity (infra) sem vazar anotações JPA para cima.
 */
@Component
class PaymentRepositoryAdapter implements PaymentRepository {

    private final PaymentJpaRepository jpaRepository;

    PaymentRepositoryAdapter(PaymentJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public void save(Payment payment) {
        jpaRepository.findByOrderId(payment.getOrderId())
                .ifPresentOrElse(
                        entity -> {
                            // Atualiza somente o status (refund).
                            entity.setStatus(payment.getStatus().name());
                        },
                        () -> jpaRepository.save(toEntity(payment))
                );
    }

    @Override
    public Optional<Payment> findByOrderId(String orderId) {
        return jpaRepository.findByOrderId(orderId).map(this::toDomain);
    }

    private PaymentJpaEntity toEntity(Payment payment) {
        return new PaymentJpaEntity(
                payment.getOrderId(),
                payment.getAmount().amount(),
                payment.getAmount().currency().getCurrencyCode(),
                payment.getStatus().name(),
                payment.getReason(),
                Instant.now());
    }

    private Payment toDomain(PaymentJpaEntity entity) {
        Money money = Money.of(entity.getAmount(), entity.getCurrency());
        PaymentStatus status = PaymentStatus.valueOf(entity.getStatus());
        return new Payment(entity.getOrderId(), money, status, entity.getReason());
    }
}
