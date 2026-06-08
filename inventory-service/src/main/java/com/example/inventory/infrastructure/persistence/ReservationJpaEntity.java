package com.example.inventory.infrastructure.persistence;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Entidade JPA de Reservation — fica restrita à camada de infraestrutura. */
@Entity
@Table(name = "reservation")
public class ReservationJpaEntity {

    @Id
    @Column(name = "order_id", nullable = false)
    private String orderId;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    // Associação unidirecional: o item carrega order_id na sua própria chave composta
    // (EmbeddedId), por isso a join column é somente-leitura aqui — quem escreve order_id
    // é o item. Antes usava mappedBy="orderId", que não é uma associação no item e fazia
    // o Hibernate falhar na validação do mapeamento (o app nem subia).
    @OneToMany(
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.EAGER)
    @JoinColumn(name = "order_id", referencedColumnName = "order_id",
            insertable = false, updatable = false)
    private List<ReservationItemJpaEntity> items = new ArrayList<>();

    protected ReservationJpaEntity() {
    }

    public ReservationJpaEntity(String orderId, String status, Instant createdAt) {
        this.orderId = orderId;
        this.status = status;
        this.createdAt = createdAt;
    }

    public String getOrderId() {
        return orderId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public List<ReservationItemJpaEntity> getItems() {
        return items;
    }

    public void setItems(List<ReservationItemJpaEntity> items) {
        this.items.clear();
        this.items.addAll(items);
    }
}
