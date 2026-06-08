package com.example.orders.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.math.BigDecimal;

/** Representação JPA de um item — NÃO é o OrderItem do domínio (tradução no adapter). */
@Embeddable
public class OrderItemEmbeddable {

    @Column(name = "sku", nullable = false)
    private String sku;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "unit_price", nullable = false)
    private BigDecimal unitPrice;

    @Column(name = "currency", nullable = false)
    private String currency;

    protected OrderItemEmbeddable() {
        // exigido pelo JPA
    }

    public OrderItemEmbeddable(String sku, int quantity, BigDecimal unitPrice, String currency) {
        this.sku = sku;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.currency = currency;
    }

    public String getSku() {
        return sku;
    }

    public int getQuantity() {
        return quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public String getCurrency() {
        return currency;
    }
}
