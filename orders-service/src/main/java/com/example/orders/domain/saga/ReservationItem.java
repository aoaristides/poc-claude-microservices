package com.example.orders.domain.saga;

import com.example.orders.domain.model.Sku;

import java.util.Objects;

/** Item a reservar no estoque. Dado mínimo que o comando ReserveStock carrega. */
public record ReservationItem(Sku sku, int quantity) {

    public ReservationItem {
        Objects.requireNonNull(sku, "sku");
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantidade deve ser positiva");
        }
    }
}
