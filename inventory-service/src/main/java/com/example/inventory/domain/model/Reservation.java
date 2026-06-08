package com.example.inventory.domain.model;

import java.time.Instant;
import java.util.List;

/**
 * Aggregate Reservation — regista os itens reservados para um pedido.
 *
 * <p>Permite que o domain service saiba o que liberar na compensação e garante
 * idempotência de negócio: se já existe RESERVED para o orderId, não decrementa de novo.
 */
public class Reservation {

    private final String orderId;
    private ReservationStatus status;
    private final List<ReservedItem> items;
    private final Instant createdAt;

    /** Construtor de criação (nova reserva). */
    public Reservation(String orderId, List<ReservedItem> items) {
        if (orderId == null || orderId.isBlank()) {
            throw new IllegalArgumentException("orderId não pode ser nulo/vazio");
        }
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Reserva deve ter ao menos um item");
        }
        this.orderId = orderId;
        this.status = ReservationStatus.RESERVED;
        this.items = List.copyOf(items);
        this.createdAt = Instant.now();
    }

    /** Construtor para restaurar do repositório. */
    public Reservation(String orderId, ReservationStatus status,
                       List<ReservedItem> items, Instant createdAt) {
        this.orderId = orderId;
        this.status = status;
        this.items = List.copyOf(items);
        this.createdAt = createdAt;
    }

    public boolean isReserved() {
        return status == ReservationStatus.RESERVED;
    }

    /** Marca a reserva como liberada. Chamado pelo domain service na compensação. */
    public void release() {
        this.status = ReservationStatus.RELEASED;
    }

    public String getOrderId() {
        return orderId;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public List<ReservedItem> getItems() {
        return items;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
