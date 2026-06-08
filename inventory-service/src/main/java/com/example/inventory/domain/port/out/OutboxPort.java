package com.example.inventory.domain.port.out;

/**
 * Porta de saída do Outbox.
 * Persiste a resposta a ser enviada ao orquestrador dentro da mesma transação.
 */
public interface OutboxPort {

    void registerStockReserved(String orderId);

    void registerStockUnavailable(String orderId, String reason);

    void registerReservationReleased(String orderId);
}
