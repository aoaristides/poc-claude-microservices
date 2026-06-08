package com.example.orders.domain.port.out;

import com.example.orders.domain.event.DomainEvent;
import com.example.orders.domain.saga.SagaCommand;

/**
 * Porta de saída do Outbox. A aplicação fala "registre esta mensagem"; o adapter
 * grava na tabela outbox na MESMA transação do aggregate. A publicação no Kafka
 * é feita depois, por um relay separado — garante atomicidade entre estado e evento.
 */
public interface OutboxPort {

    /** Registra um comando da saga destinado a um participante (Inventory/Payment/Shipping). */
    void register(SagaCommand command);

    /** Registra um evento de domínio para publicação ampla (orders.events). */
    void register(DomainEvent event);
}
