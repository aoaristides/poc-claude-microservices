package com.example.shipping.domain.port.in;

import java.util.List;

/**
 * Porta de entrada: agenda uma entrega para o pedido recebido via comando da saga.
 *
 * <p>A implementação é responsável pela idempotência: se o orderId já foi processado,
 * re-publica o resultado anterior sem reprocessar a regra de negócio.
 */
public interface ScheduleDeliveryUseCase {

    /**
     * @param orderId identificador do pedido originado pelo orquestrador
     * @param skus    SKUs do pedido; a política decide a entregabilidade a partir deles
     */
    void execute(String orderId, List<String> skus);
}
