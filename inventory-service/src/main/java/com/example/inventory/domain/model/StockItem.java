package com.example.inventory.domain.model;

import com.example.inventory.domain.exception.InsufficientStockException;

/**
 * Aggregate StockItem — unidade de estoque identificada por SKU.
 *
 * <p>Invariante: {@code availableQuantity} nunca é negativo. Os únicos pontos de mutação
 * são {@link #reserve} e {@link #release}; sem setters públicos.
 */
public class StockItem {

    private final String sku;
    private int availableQuantity;

    /** Construtor para restaurar do repositório. */
    public StockItem(String sku, int availableQuantity) {
        if (sku == null || sku.isBlank()) {
            throw new IllegalArgumentException("SKU não pode ser nulo/vazio");
        }
        if (availableQuantity < 0) {
            throw new IllegalArgumentException("Quantidade disponível não pode ser negativa");
        }
        this.sku = sku;
        this.availableQuantity = availableQuantity;
    }

    /**
     * Decrementa o estoque em {@code qty} unidades.
     *
     * @throws InsufficientStockException se {@code qty} excede o disponível
     */
    public void reserve(int qty) {
        if (qty <= 0) {
            throw new IllegalArgumentException("Quantidade a reservar deve ser positiva: " + qty);
        }
        if (qty > availableQuantity) {
            throw new InsufficientStockException(sku, availableQuantity, qty);
        }
        availableQuantity -= qty;
    }

    /**
     * Incrementa o estoque em {@code qty} unidades (estorna reserva).
     */
    public void release(int qty) {
        if (qty <= 0) {
            throw new IllegalArgumentException("Quantidade a liberar deve ser positiva: " + qty);
        }
        availableQuantity += qty;
    }

    public String getSku() {
        return sku;
    }

    public int getAvailableQuantity() {
        return availableQuantity;
    }
}
