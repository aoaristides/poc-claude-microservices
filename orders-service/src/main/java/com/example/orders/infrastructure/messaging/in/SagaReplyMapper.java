package com.example.orders.infrastructure.messaging.in;

import com.example.orders.domain.model.OrderId;
import com.example.orders.domain.saga.SagaReply;
import com.example.orders.domain.saga.SagaReplyType;
import com.example.orders.infrastructure.messaging.InvalidEventException;
import com.example.orders.infrastructure.messaging.contract.SagaReplyPayload;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.stereotype.Component;

/**
 * Anti-Corruption Layer de entrada: traduz a mensagem do broker (header eventType +
 * payload JSON) para a linguagem do domínio ({@link SagaReply}). O domínio não conhece
 * o nome publicado dos eventos dos participantes.
 */
@Component
class SagaReplyMapper {

    private final ObjectMapper mapper;

    SagaReplyMapper(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    SagaReply toDomain(ConsumerRecord<String, String> record) {
        String eventType = header(record, "eventType");
        SagaReplyType type = mapType(eventType);
        SagaReplyPayload payload = parse(record.value());

        OrderId orderId = OrderId.of(payload.orderId());
        return SagaReply.withReason(orderId, type, payload.reason());
    }

    private SagaReplyType mapType(String eventType) {
        return switch (eventType) {
            case "StockReserved" -> SagaReplyType.STOCK_RESERVED;
            case "StockUnavailable" -> SagaReplyType.STOCK_UNAVAILABLE;
            case "PaymentAuthorized" -> SagaReplyType.PAYMENT_AUTHORIZED;
            case "PaymentDeclined" -> SagaReplyType.PAYMENT_DECLINED;
            case "PaymentRefunded" -> SagaReplyType.PAYMENT_REFUNDED;
            case "DeliveryScheduled" -> SagaReplyType.DELIVERY_SCHEDULED;
            case "DeliveryFailed" -> SagaReplyType.DELIVERY_FAILED;
            case "ReservationReleased" -> SagaReplyType.RESERVATION_RELEASED;
            default -> throw new InvalidEventException("eventType desconhecido: " + eventType);
        };
    }

    private SagaReplyPayload parse(String value) {
        try {
            return mapper.readValue(value, SagaReplyPayload.class);
        } catch (Exception e) {
            throw new InvalidEventException("payload de resposta inválido", e);
        }
    }

    private String header(ConsumerRecord<String, String> record, String name) {
        var h = record.headers().lastHeader(name);
        if (h == null) {
            throw new InvalidEventException("header ausente: " + name);
        }
        return new String(h.value(), java.nio.charset.StandardCharsets.UTF_8);
    }
}
