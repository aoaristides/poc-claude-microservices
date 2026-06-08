package com.example.orders.application.service;

import com.example.orders.domain.exception.SagaNotFoundException;
import com.example.orders.domain.model.OrderId;
import com.example.orders.domain.port.in.RecoverTimedOutSagaUseCase;
import com.example.orders.domain.port.out.SagaRepository;
import com.example.orders.domain.saga.CheckoutSaga;
import com.example.orders.domain.saga.SagaCommand;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Application service: recupera uma saga que ficou presa aguardando um participante.
 *
 * <p>Uma transação por saga (chamada em loop pelo scheduler) — evita transação longa.
 * O domínio decide a recuperação ({@link CheckoutSaga#onTimeout}); aqui só orquestramos.
 */
@Service
public class SagaTimeoutService implements RecoverTimedOutSagaUseCase {

    private static final String TIMEOUT_REASON = "saga timeout: participante não respondeu a tempo";

    private final SagaRepository sagas;
    private final SagaCommandDispatcher dispatcher;

    public SagaTimeoutService(SagaRepository sagas, SagaCommandDispatcher dispatcher) {
        this.sagas = sagas;
        this.dispatcher = dispatcher;
    }

    @Override
    @Transactional
    public void execute(OrderId orderId) {
        CheckoutSaga saga = sagas.findByOrderId(orderId)
                .orElseThrow(() -> new SagaNotFoundException(orderId));

        // Corrida benigna: pode ter finalizado entre a seleção e a execução.
        if (saga.isFinished()) {
            return;
        }

        List<SagaCommand> commands = saga.onTimeout(TIMEOUT_REASON);

        sagas.save(saga);
        dispatcher.dispatch(commands);
    }
}
