package com.example.inventory.domain.exception;

/**
 * Exceção de domínio: SKU não cadastrado no estoque.
 */
public class StockItemNotFoundException extends RuntimeException {

    public StockItemNotFoundException(String sku) {
        super("SKU não encontrado: " + sku);
    }
}
