package com.example.inventory.infrastructure.messaging.in;

import com.example.inventory.application.command.ReleaseReservationCommand;
import com.example.inventory.application.command.ReserveStockCommand;
import com.example.inventory.infrastructure.messaging.InvalidCommandException;
import com.example.inventory.infrastructure.messaging.contract.OrderRefMessage;
import com.example.inventory.infrastructure.messaging.contract.ReserveStockMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * Anti-Corruption Layer de entrada: traduz a mensagem do broker (header eventType +
 * payload JSON) para comandos da camada de aplicação. O domínio não conhece o
 * protocolo de mensageria.
 */
@Component
class InventoryCommandMapper {

    private final ObjectMapper mapper;

    InventoryCommandMapper(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    /** Tipo de comando resolvido a partir do header {@code eventType}. */
    enum CommandType {
        RESERVE_STOCK, RELEASE_RESERVATION
    }

    CommandType resolveType(ConsumerRecord<String, String> record) {
        String eventType = header(record, "eventType");
        return switch (eventType) {
            case "ReserveStock" -> CommandType.RESERVE_STOCK;
            case "ReleaseReservation" -> CommandType.RELEASE_RESERVATION;
            default -> throw new InvalidCommandException("eventType desconhecido: " + eventType);
        };
    }

    ReserveStockCommand toReserveCommand(ConsumerRecord<String, String> record) {
        ReserveStockMessage msg = parse(record.value(), ReserveStockMessage.class);
        var items = msg.items().stream()
                .map(i -> new ReserveStockCommand.Item(i.sku(), i.quantity()))
                .toList();
        return new ReserveStockCommand(msg.orderId(), items);
    }

    ReleaseReservationCommand toReleaseCommand(ConsumerRecord<String, String> record) {
        OrderRefMessage msg = parse(record.value(), OrderRefMessage.class);
        return new ReleaseReservationCommand(msg.orderId());
    }

    private <T> T parse(String value, Class<T> type) {
        try {
            return mapper.readValue(value, type);
        } catch (Exception e) {
            throw new InvalidCommandException("payload inválido para " + type.getSimpleName(), e);
        }
    }

    private String header(ConsumerRecord<String, String> record, String name) {
        var h = record.headers().lastHeader(name);
        if (h == null) {
            throw new InvalidCommandException("header ausente: " + name);
        }
        return new String(h.value(), StandardCharsets.UTF_8);
    }
}
