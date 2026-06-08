package com.example.inventory.infrastructure.messaging.contract;

import java.util.List;

/**
 * Contrato de entrada: payload do comando ReserveStock enviado pelo orquestrador.
 * Record imutável — ACL de entrada do serviço.
 */
public record ReserveStockMessage(String orderId, List<Item> items) {

    public record Item(String sku, int quantity) {}
}
