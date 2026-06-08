package com.example.orders.infrastructure.messaging.contract;

import java.util.List;

/**
 * Contrato publicado do comando ReserveStock (Published Language).
 * Separado dos objetos de domínio: a evolução do schema não vaza para o domínio.
 */
public record ReserveStockMessage(String orderId, List<Item> items) {

    public record Item(String sku, int quantity) {
    }
}
