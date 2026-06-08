package com.example.orders.infrastructure.persistence;

import com.example.orders.domain.saga.SagaState;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Entidade JPA do estado da saga. */
@Entity
@Table(name = "checkout_saga")
public class SagaJpaEntity {

    @Id
    private UUID id;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SagaState state;

    @Column(name = "total_amount", nullable = false)
    private BigDecimal totalAmount;

    @Column(name = "total_currency", nullable = false)
    private String totalCurrency;

    @Column(name = "failure_reason")
    private String failureReason;

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // Passos concluídos guardados como String (nome do enum) na tabela saga_step.
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "saga_step", joinColumns = @JoinColumn(name = "saga_id"))
    @Column(name = "step", nullable = false)
    private Set<String> completedSteps = new HashSet<>();

    protected SagaJpaEntity() {
    }

    public SagaJpaEntity(UUID id, UUID orderId, SagaState state,
                         BigDecimal totalAmount, String totalCurrency,
                         String failureReason, Instant updatedAt, Set<String> completedSteps) {
        this.id = id;
        this.orderId = orderId;
        this.state = state;
        this.totalAmount = totalAmount;
        this.totalCurrency = totalCurrency;
        this.failureReason = failureReason;
        this.updatedAt = updatedAt;
        this.completedSteps = new HashSet<>(completedSteps);
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public SagaState getState() {
        return state;
    }

    public void setState(SagaState state) {
        this.state = state;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public String getTotalCurrency() {
        return totalCurrency;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Set<String> getCompletedSteps() {
        return completedSteps;
    }

    public void setCompletedSteps(Set<String> completedSteps) {
        this.completedSteps = completedSteps;
    }
}
