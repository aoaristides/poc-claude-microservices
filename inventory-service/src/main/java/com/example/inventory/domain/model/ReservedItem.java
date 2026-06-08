package com.example.inventory.domain.model;

/**
 * Value Object que representa um item dentro de uma Reservation.
 * Grava o que foi reservado para permitir liberar o estoque corretamente.
 */
public record ReservedItem(String sku, int quantity) {

    public ReservedItem {
        if (sku == null || sku.isBlank()) {
            throw new IllegalArgumentException("SKU não pode ser nulo/vazio");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantidade deve ser positiva: " + quantity);
        }
    }
}
