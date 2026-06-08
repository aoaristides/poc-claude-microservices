package com.example.inventory.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/** Entidade JPA de StockItem — fica restrita à camada de infraestrutura. */
@Entity
@Table(name = "stock_item")
public class StockItemJpaEntity {

    @Id
    @Column(name = "sku", nullable = false)
    private String sku;

    @Column(name = "available_quantity", nullable = false)
    private int availableQuantity;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected StockItemJpaEntity() {
    }

    public StockItemJpaEntity(String sku, int availableQuantity) {
        this.sku = sku;
        this.availableQuantity = availableQuantity;
    }

    public String getSku() {
        return sku;
    }

    public int getAvailableQuantity() {
        return availableQuantity;
    }

    public void setAvailableQuantity(int availableQuantity) {
        this.availableQuantity = availableQuantity;
    }

    public long getVersion() {
        return version;
    }
}
