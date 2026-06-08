package com.example.shipping.domain.port.in;

/**
 * Porta de entrada: agenda uma entrega para o pedido recebido via comando da saga.
 *
 * <p>A implementação é responsável pela idempotência: se o orderId já foi processado,
 * re-publica o resultado anterior sem reprocessar a regra de negócio.
 */
public interface ScheduleDeliveryUseCase {

    /** @param orderId identificador do pedido originado pelo orquestrador */
    void execute(String orderId);
}
