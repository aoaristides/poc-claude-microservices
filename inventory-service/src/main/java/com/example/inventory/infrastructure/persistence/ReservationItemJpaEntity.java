package com.example.inventory.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.util.Objects;

/** Entidade JPA de item de reserva — chave composta (order_id, sku). */
@Entity
@Table(name = "reservation_item")
public class ReservationItemJpaEntity {

    @EmbeddedId
    private ReservationItemId id;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    protected ReservationItemJpaEntity() {
    }

    public ReservationItemJpaEntity(String orderId, String sku, int quantity) {
        this.id = new ReservationItemId(orderId, sku);
        this.quantity = quantity;
    }

    public String getOrderId() {
        return id.orderId;
    }

    public String getSku() {
        return id.sku;
    }

    public int getQuantity() {
        return quantity;
    }

    @Embeddable
    public static class ReservationItemId implements Serializable {

        @Column(name = "order_id")
        private String orderId;

        @Column(name = "sku")
        private String sku;

        protected ReservationItemId() {
        }

        public ReservationItemId(String orderId, String sku) {
            this.orderId = orderId;
            this.sku = sku;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ReservationItemId that)) return false;
            return Objects.equals(orderId, that.orderId) && Objects.equals(sku, that.sku);
        }

        @Override
        public int hashCode() {
            return Objects.hash(orderId, sku);
        }
    }
}
