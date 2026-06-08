package com.example.orders.infrastructure.scheduling;

import com.example.orders.domain.model.OrderId;
import com.example.orders.domain.port.in.RecoverTimedOutSagaUseCase;
import com.example.orders.domain.port.out.SagaRepository;
import com.example.orders.infrastructure.config.OrdersSagaProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

/**
 * Varre periodicamente sagas presas e dispara a recuperação.
 *
 * <p>A varredura é só leitura; a recuperação de cada saga roda em sua própria
 * transação (no use case). Falha em uma saga não impede as outras.
 */
@Component
class SagaTimeoutScheduler {

    private static final Logger log = LoggerFactory.getLogger(SagaTimeoutScheduler.class);

    private final SagaRepository sagas;
    private final RecoverTimedOutSagaUseCase recoverUseCase;
    private final OrdersSagaProperties properties;

    SagaTimeoutScheduler(SagaRepository sagas,
                         RecoverTimedOutSagaUseCase recoverUseCase,
                         OrdersSagaProperties properties) {
        this.sagas = sagas;
        this.recoverUseCase = recoverUseCase;
        this.properties = properties;
    }

    @Scheduled(fixedDelayString = "${orders.saga.timeout-check-delay-ms}")
    public void recoverStaleSagas() {
        Instant deadline = Instant.now().minusMillis(properties.timeoutMs());
        List<OrderId> stale = sagas.findStaleOrderIds(deadline);
        if (stale.isEmpty()) {
            return;
        }
        log.warn("recuperando {} saga(s) presa(s) por timeout", stale.size());
        for (OrderId orderId : stale) {
            try {
                recoverUseCase.execute(orderId);
            } catch (Exception e) {
                // ALERTA: não interrompe as demais; investigar manualmente se persistir.
                log.error("falha recuperando saga do pedido {}", orderId.asString(), e);
            }
        }
    }
}
