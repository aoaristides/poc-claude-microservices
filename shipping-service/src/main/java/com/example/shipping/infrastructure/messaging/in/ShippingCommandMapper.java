package com.example.shipping.infrastructure.messaging.in;

import com.example.shipping.infrastructure.messaging.InvalidEventException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * Anti-Corruption Layer de entrada: traduz a mensagem do broker para a linguagem
 * do domínio (orderId). O domínio não conhece detalhes do protocolo Kafka.
 */
@Component
class ShippingCommandMapper {

    private final ObjectMapper mapper;

    ShippingCommandMapper(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * Extrai o orderId do record Kafka.
     *
     * <p>Valida que o header {@code eventType} é {@code ScheduleDelivery} e que
     * o payload contém um {@code orderId} válido.
     */
    String toOrderId(ConsumerRecord<String, String> record) {
        String eventType = header(record, "eventType");
        if (!"ScheduleDelivery".equals(eventType)) {
            throw new InvalidEventException("eventType inesperado: " + eventType);
        }
        ShippingCommandPayload payload = parse(record.value());
        if (payload.orderId() == null || payload.orderId().isBlank()) {
            throw new InvalidEventException("orderId ausente no payload ScheduleDelivery");
        }
        return payload.orderId();
    }

    private ShippingCommandPayload parse(String value) {
        try {
            return mapper.readValue(value, ShippingCommandPayload.class);
        } catch (Exception e) {
            throw new InvalidEventException("payload de comando inválido", e);
        }
    }

    private String header(ConsumerRecord<String, String> record, String name) {
        var h = record.headers().lastHeader(name);
        if (h == null) {
            throw new InvalidEventException("header ausente: " + name);
        }
        return new String(h.value(), StandardCharsets.UTF_8);
    }
}
