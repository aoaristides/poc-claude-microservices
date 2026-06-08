package com.example.orders.application.service;

import com.example.orders.domain.exception.SagaNotFoundException;
import com.example.orders.domain.port.in.ProcessSagaReplyUseCase;
import com.example.orders.domain.port.out.SagaRepository;
import com.example.orders.domain.saga.CheckoutSaga;
import com.example.orders.domain.saga.SagaCommand;
import com.example.orders.domain.saga.SagaReply;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Application service: processa a resposta de um participante e avança a saga.
 *
 * <p>Carrega a saga, deixa o DOMÍNIO decidir a transição e os próximos comandos,
 * persiste a saga e despacha os comandos via {@link SagaCommandDispatcher}.
 * Tudo na mesma transação.
 */
@Service
public class ProcessSagaReplyService implements ProcessSagaReplyUseCase {

    private final SagaRepository sagas;
    private final SagaCommandDispatcher dispatcher;

    public ProcessSagaReplyService(SagaRepository sagas, SagaCommandDispatcher dispatcher) {
        this.sagas = sagas;
        this.dispatcher = dispatcher;
    }

    @Override
    @Transactional
    public void execute(SagaReply reply) {
        CheckoutSaga saga = sagas.findByOrderId(reply.orderId())
                .orElseThrow(() -> new SagaNotFoundException(reply.orderId()));

        List<SagaCommand> nextCommands = saga.apply(reply);

        sagas.save(saga);
        dispatcher.dispatch(nextCommands);
    }
}
