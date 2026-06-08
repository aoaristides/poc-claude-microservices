package com.example.inventory.application.command;

import java.util.List;

/**
 * Comando de aplicação: reservar estoque para um pedido.
 *
 * @param orderId identificador do pedido (chave de idempotência de negócio)
 * @param items   itens a reservar
 */
public record ReserveStockCommand(String orderId, List<Item> items) {

    public record Item(String sku, int quantity) {}
}
