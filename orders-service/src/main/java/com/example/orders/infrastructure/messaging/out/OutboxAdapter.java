package com.example.orders.infrastructure.messaging.out;

import com.example.orders.domain.event.DomainEvent;
import com.example.orders.domain.event.OrderCancelled;
import com.example.orders.domain.event.OrderPaid;
import com.example.orders.domain.port.out.OutboxPort;
import com.example.orders.domain.saga.SagaCommand;
import com.example.orders.infrastructure.config.OrdersTopicsProperties;
import com.example.orders.infrastructure.messaging.EventSerializationException;
import com.example.orders.infrastructure.messaging.contract.AuthorizePaymentMessage;
import com.example.orders.infrastructure.messaging.contract.OrderCancelledMessage;
import com.example.orders.infrastructure.messaging.contract.OrderPaidMessage;
import com.example.orders.infrastructure.messaging.contract.OrderRefMessage;
import com.example.orders.infrastructure.messaging.contract.ReserveStockMessage;
import com.example.orders.infrastructure.messaging.contract.ScheduleDeliveryMessage;
import com.example.orders.infrastructure.persistence.OutboxJpaEntity;
import com.example.orders.infrastructure.persistence.OutboxJpaRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/**
 * Adapter de saída do Outbox.
 *
 * <p>Traduz comando/evento de DOMÍNIO para o contrato publicado (Published Language),
 * resolve tópico e chave, serializa e grava na tabela outbox — tudo dentro da
 * transação do caller. A publicação real no Kafka fica para o {@link OutboxRelay}.
 */
@Component
class OutboxAdapter implements OutboxPort {

    private static final String AGGREGATE_TYPE = "Order";

    private final OutboxJpaRepository repo;
    private final OrdersTopicsProperties topics;
    private final ObjectMapper mapper;

    OutboxAdapter(OutboxJpaRepository repo, OrdersTopicsProperties topics, ObjectMapper mapper) {
        this.repo = repo;
        this.topics = topics;
        this.mapper = mapper;
    }

    @Override
    public void register(SagaCommand command) {
        String orderId = command.orderId().asString();
        String topic = topicFor(command);
        String eventType = eventTypeFor(command.type());
        String payload = serialize(payloadFor(command));
        repo.save(newEntry(topic, orderId, eventType, payload));
    }

    @Override
    public void register(DomainEvent event) {
        String orderId;
        String eventType;
        Object payload;

        if (event instanceof OrderPaid e) {
            orderId = e.orderId().asString();
            eventType = "OrderPaid";
            payload = new OrderPaidMessage(orderId, e.clientId().asString(), e.occurredAt());
        } else if (event instanceof OrderCancelled e) {
            orderId = e.orderId().asString();
            eventType = "OrderCancelled";
            payload = new OrderCancelledMessage(orderId, e.reason(), e.occurredAt());
        } else {
            throw new IllegalArgumentException("evento de domínio não mapeado: " + event.getClass());
        }

        repo.save(newEntry(topics.orderEvents(), orderId, eventType, serialize(payload)));
    }

    // ----------------------------- mapeamentos -----------------------------

    private String topicFor(SagaCommand command) {
        return switch (command.type()) {
            case RESERVE_STOCK, RELEASE_RESERVATION -> topics.inventoryCommands();
            case AUTHORIZE_PAYMENT, REFUND_PAYMENT -> topics.paymentCommands();
            case SCHEDULE_DELIVERY -> topics.shippingCommands();
            // comandos internos não passam por aqui (tratados no application service)
            case CONFIRM_ORDER_PAYMENT, CANCEL_ORDER ->
                    throw new IllegalArgumentException("comando interno não vai para o outbox: " + command.type());
        };
    }

    private String eventTypeFor(SagaCommand.Type type) {
        return switch (type) {
            case RESERVE_STOCK -> "ReserveStock";
            case RELEASE_RESERVATION -> "ReleaseReservation";
            case AUTHORIZE_PAYMENT -> "AuthorizePayment";
            case REFUND_PAYMENT -> "RefundPayment";
            case SCHEDULE_DELIVERY -> "ScheduleDelivery";
            case CONFIRM_ORDER_PAYMENT, CANCEL_ORDER ->
                    throw new IllegalArgumentException("comando interno sem contrato externo: " + type);
        };
    }

    private Object payloadFor(SagaCommand command) {
        String orderId = command.orderId().asString();
        return switch (command.type()) {
            case RESERVE_STOCK -> new ReserveStockMessage(orderId,
                    command.items().stream()
                            .map(i -> new ReserveStockMessage.Item(i.sku().value(), i.quantity()))
                            .toList());
            case AUTHORIZE_PAYMENT -> new AuthorizePaymentMessage(orderId,
                    command.amount().amount(),
                    command.amount().currency().getCurrencyCode());
            case SCHEDULE_DELIVERY -> new ScheduleDeliveryMessage(orderId,
                    command.items().stream()
                            .map(i -> new ScheduleDeliveryMessage.Item(i.sku().value(), i.quantity()))
                            .toList());
            case RELEASE_RESERVATION, REFUND_PAYMENT -> new OrderRefMessage(orderId);
            case CONFIRM_ORDER_PAYMENT, CANCEL_ORDER ->
                    throw new IllegalArgumentException("comando interno sem payload externo: " + command.type());
        };
    }

    private OutboxJpaEntity newEntry(String topic, String orderId, String eventType, String payload) {
        return new OutboxJpaEntity(
                UUID.randomUUID(),   // id = messageId usado para dedup no consumer
                AGGREGATE_TYPE,
                orderId,
                topic,
                orderId,             // msgKey = orderId garante ordem por partição
                eventType,
                payload,
                Instant.now());
    }

    private String serialize(Object payload) {
        try {
            return mapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new EventSerializationException("falha serializando payload do outbox", e);
        }
    }
}
