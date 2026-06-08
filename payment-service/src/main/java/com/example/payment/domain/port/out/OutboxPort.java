package com.example.payment.domain.port.out;

/** Porta de saída: registra mensagem de resposta no outbox (dentro da transação corrente). */
public interface OutboxPort {

    /**
     * Registra uma resposta PaymentAuthorized no outbox.
     *
     * @param orderId identificador do pedido
     */
    void registerAuthorized(String orderId);

    /**
     * Registra uma resposta PaymentDeclined no outbox.
     *
     * @param orderId identificador do pedido
     * @param reason  motivo da recusa
     */
    void registerDeclined(String orderId, String reason);

    /**
     * Registra uma resposta PaymentRefunded no outbox.
     *
     * @param orderId identificador do pedido
     */
    void registerRefunded(String orderId);
}
