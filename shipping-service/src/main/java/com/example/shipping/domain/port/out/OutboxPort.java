package com.example.shipping.domain.port.out;

import com.example.shipping.domain.model.Delivery;

/**
 * Porta de saída: registra a resposta da saga no outbox.
 *
 * <p>O adapter persiste a entrada na tabela outbox dentro da MESMA transação
 * do application service. A publicação real no Kafka é feita pelo OutboxRelay.
 */
public interface OutboxPort {

    /**
     * Registra a resposta a ser publicada no tópico de replies.
     *
     * @param delivery aggregate com status final após o agendamento
     */
    void registerReply(Delivery delivery);
}
