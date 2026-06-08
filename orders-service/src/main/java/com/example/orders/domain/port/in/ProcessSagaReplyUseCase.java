package com.example.orders.domain.port.in;

import com.example.orders.domain.saga.SagaReply;

/** Porta de entrada: processar uma resposta de participante e avançar a saga. */
public interface ProcessSagaReplyUseCase {

    void execute(SagaReply reply);
}
