package com.example.inventory.api;

import com.example.inventory.domain.model.StockItem;
import com.example.inventory.domain.port.out.StockItemRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint de leitura para debug operacional.
 * Não faz parte do fluxo da saga — serve apenas para inspeção manual.
 */
@RestController
@RequestMapping("/stock")
public class StockController {

    private final StockItemRepository stockItemRepository;

    public StockController(StockItemRepository stockItemRepository) {
        this.stockItemRepository = stockItemRepository;
    }

    @GetMapping("/{sku}")
    public ResponseEntity<StockResponse> getStock(@PathVariable String sku) {
        return stockItemRepository.findBySku(sku)
                .map(item -> ResponseEntity.ok(toResponse(item)))
                .orElse(ResponseEntity.notFound().build());
    }

    private StockResponse toResponse(StockItem item) {
        return new StockResponse(item.getSku(), item.getAvailableQuantity());
    }

    /** DTO de resposta — entidade de domínio não sai do service. */
    public record StockResponse(String sku, int availableQuantity) {}
}
