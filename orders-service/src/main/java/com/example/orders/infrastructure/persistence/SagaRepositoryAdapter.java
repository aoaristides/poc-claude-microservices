package com.example.orders.infrastructure.persistence;

import com.example.orders.domain.model.Money;
import com.example.orders.domain.model.OrderId;
import com.example.orders.domain.saga.CheckoutSaga;
import com.example.orders.domain.saga.SagaId;
import com.example.orders.domain.saga.SagaState;
import com.example.orders.domain.saga.SagaStep;
import com.example.orders.domain.port.out.SagaRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/** Adapter de persistência da saga. Traduz domínio &lt;-&gt; JPA. */
@Component
class SagaRepositoryAdapter implements SagaRepository {

    private final SagaJpaRepository repo;

    SagaRepositoryAdapter(SagaJpaRepository repo) {
        this.repo = repo;
    }

    @Override
    public Optional<CheckoutSaga> findByOrderId(OrderId orderId) {
        return repo.findByOrderId(orderId.value()).map(this::toDomain);
    }

    @Override
    public void save(CheckoutSaga saga) {
        var stepNames = saga.completedSteps().stream()
                .map(Enum::name)
                .collect(Collectors.toSet());

        var existing = repo.findById(saga.id().value()).orElse(null);
        if (existing != null) {
            // Update: estado, motivo e passos concluídos podem mudar.
            existing.setState(saga.state());
            existing.setFailureReason(saga.failureReason());
            existing.setCompletedSteps(stepNames);
            existing.setUpdatedAt(Instant.now());
            repo.save(existing);
            return;
        }

        var amount = saga.totalAmount();
        var entity = new SagaJpaEntity(
                saga.id().value(),
                saga.orderId().value(),
                saga.state(),
                amount.amount(),
                amount.currency().getCurrencyCode(),
                saga.failureReason(),
                Instant.now(),
                stepNames);
        repo.save(entity);
    }

    @Override
    public List<OrderId> findStaleOrderIds(Instant updatedBefore) {
        // Estados terminais ficam de fora: não há o que recuperar neles.
        var terminal = EnumSet.of(SagaState.COMPLETED, SagaState.FAILED);
        return repo.findStaleOrderIds(terminal, updatedBefore).stream()
                .map(OrderId::new)
                .toList();
    }

    private CheckoutSaga toDomain(SagaJpaEntity e) {
        Set<SagaStep> steps = e.getCompletedSteps().isEmpty()
                ? EnumSet.noneOf(SagaStep.class)
                : e.getCompletedSteps().stream()
                    .map(SagaStep::valueOf)
                    .collect(Collectors.toCollection(() -> EnumSet.noneOf(SagaStep.class)));

        return CheckoutSaga.rehydrate(
                new SagaId(e.getId()),
                new OrderId(e.getOrderId()),
                Money.of(e.getTotalAmount(), e.getTotalCurrency()),
                e.getState(),
                steps,
                e.getFailureReason());
    }
}
