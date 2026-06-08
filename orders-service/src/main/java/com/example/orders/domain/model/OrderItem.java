package com.example.orders.domain.model;

import java.util.Objects;

/**
 * Item do pedido. Entidade interna do aggregate {@link Order} — só deve ser
 * manipulada através da raiz. Modelada como record imutável (substituição, não mutação).
 */
public record OrderItem(Sku sku, int quantity, Money unitPrice) {

    public OrderItem {
        Objects.requireNonNull(sku, "sku");
        Objects.requireNonNull(unitPrice, "unitPrice");
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantidade deve ser positiva");
        }
    }

    /** Subtotal da linha = preço unitário x quantidade. */
    public Money subtotal() {
        return unitPrice.multiply(quantity);
    }
}
