package com.example.orders.application.service;

import com.example.orders.domain.exception.OrderNotFoundException;
import com.example.orders.domain.model.Order;
import com.example.orders.domain.port.out.OrderRepository;
import com.example.orders.domain.port.out.OutboxPort;
import com.example.orders.domain.saga.SagaCommand;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Despacha os comandos produzidos por uma transição da saga.
 *
 * <p>Comandos internos (CONFIRM_ORDER_PAYMENT / CANCEL_ORDER) mutam o aggregate Order
 * e geram eventos de domínio; os demais vão para o outbox rumo aos participantes.
 * Extraído para ser reusado por {@code ProcessSagaReplyService} e pelo timeout.
 */
@Component
class SagaCommandDispatcher {

    private final OrderRepository orders;
    private final OutboxPort outbox;

    SagaCommandDispatcher(OrderRepository orders, OutboxPort outbox) {
        this.orders = orders;
        this.outbox = outbox;
    }

    void dispatch(List<SagaCommand> commands) {
        for (SagaCommand command : commands) {
            if (command.isInternal()) {
                applyInternal(command);
            } else {
                outbox.register(command);
            }
        }
    }

    private void applyInternal(SagaCommand command) {
        Order order = orders.findById(command.orderId())
                .orElseThrow(() -> new OrderNotFoundException(command.orderId()));

        switch (command.type()) {
            case CONFIRM_ORDER_PAYMENT -> {
                var event = order.markPaid();
                orders.save(order);
                outbox.register(event);          // OrderPaid -> orders.events.v1
            }
            case CANCEL_ORDER -> {
                var event = order.cancel(command.reason());
                orders.save(order);
                outbox.register(event);          // OrderCancelled -> orders.events.v1
            }
            default -> throw new IllegalStateException(
                    "comando não-interno em applyInternal: " + command.type());
        }
    }
}
