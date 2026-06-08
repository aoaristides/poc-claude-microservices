package com.example.inventory.domain.port.out;

import com.example.inventory.domain.model.StockItem;

import java.util.List;
import java.util.Optional;

/** Porta de saída: persistência de StockItem. */
public interface StockItemRepository {

    Optional<StockItem> findBySku(String sku);

    /** Busca múltiplos SKUs em uma única consulta (evita N+1). */
    List<StockItem> findBySkus(List<String> skus);

    void save(StockItem stockItem);

    void saveAll(List<StockItem> stockItems);
}
