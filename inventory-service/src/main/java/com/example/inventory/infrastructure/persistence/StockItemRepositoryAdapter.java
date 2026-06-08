package com.example.inventory.infrastructure.persistence;

import com.example.inventory.domain.model.StockItem;
import com.example.inventory.domain.port.out.StockItemRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Adapter de persistência para StockItem.
 * Traduz entre o modelo de domínio e a entidade JPA — o domínio não conhece JPA.
 */
@Component
public class StockItemRepositoryAdapter implements StockItemRepository {

    private final StockItemJpaRepository jpa;

    public StockItemRepositoryAdapter(StockItemJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<StockItem> findBySku(String sku) {
        return jpa.findById(sku).map(this::toDomain);
    }

    @Override
    public List<StockItem> findBySkus(List<String> skus) {
        return jpa.findBySkuIn(skus).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public void save(StockItem stockItem) {
        var entity = jpa.findById(stockItem.getSku())
                .orElseGet(() -> new StockItemJpaEntity(stockItem.getSku(), 0));
        entity.setAvailableQuantity(stockItem.getAvailableQuantity());
        jpa.save(entity);
    }

    @Override
    public void saveAll(List<StockItem> stockItems) {
        // Carrega as entidades gerenciadas e atualiza a quantidade
        List<String> skus = stockItems.stream().map(StockItem::getSku).toList();
        var managed = jpa.findBySkuIn(skus);
        var managedMap = new java.util.HashMap<String, StockItemJpaEntity>();
        for (var e : managed) {
            managedMap.put(e.getSku(), e);
        }
        for (StockItem domain : stockItems) {
            var entity = managedMap.get(domain.getSku());
            if (entity != null) {
                entity.setAvailableQuantity(domain.getAvailableQuantity());
            }
        }
        jpa.saveAll(managed);
    }

    private StockItem toDomain(StockItemJpaEntity entity) {
        return new StockItem(entity.getSku(), entity.getAvailableQuantity());
    }
}
