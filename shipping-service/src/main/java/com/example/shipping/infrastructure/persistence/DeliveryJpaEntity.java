package com.example.shipping.infrastructure.persistence;

import com.example.shipping.domain.model.DeliveryStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;

/** Mapeamento JPA do aggregate Delivery. Não é exposto fora da camada de infraestrutura. */
@Entity
@Table(name = "delivery")
public class DeliveryJpaEntity {

    @Id
    @Column(name = "order_id", nullable = false)
    private String orderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeliveryStatus status;

    @Column(name = "tracking_code")
    private String trackingCode;

    @Column
    private String reason;

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected DeliveryJpaEntity() {
    }

    public DeliveryJpaEntity(String orderId, DeliveryStatus status,
                             String trackingCode, String reason, Instant createdAt) {
        this.orderId = orderId;
        this.status = status;
        this.trackingCode = trackingCode;
        this.reason = reason;
        this.createdAt = createdAt;
    }

    public String getOrderId() {
        return orderId;
    }

    public DeliveryStatus getStatus() {
        return status;
    }

    public String getTrackingCode() {
        return trackingCode;
    }

    public String getReason() {
        return reason;
    }

    public Long getVersion() {
        return version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
