package com.example.inventory.infrastructure.messaging.out;

import com.example.inventory.domain.port.out.OutboxPort;
import com.example.inventory.infrastructure.config.InventoryTopicsProperties;
import com.example.inventory.infrastructure.messaging.EventSerializationException;
import com.example.inventory.infrastructure.messaging.contract.ReservationReleasedMessage;
import com.example.inventory.infrastructure.messaging.contract.StockReservedMessage;
import com.example.inventory.infrastructure.messaging.contract.StockUnavailableMessage;
import com.example.inventory.infrastructure.persistence.OutboxJpaEntity;
import com.example.inventory.infrastructure.persistence.OutboxJpaRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/**
 * Adapter de saída do Outbox.
 *
 * <p>Traduz a intenção de resposta (StockReserved, StockUnavailable, ReservationReleased)
 * para o contrato publicado (Published Language), serializa e grava na tabela outbox —
 * tudo dentro da transação do caller. A publicação real no Kafka fica para o
 * {@link OutboxRelay}.
 */
@Component
class OutboxAdapter implements OutboxPort {

    private static final String AGGREGATE_TYPE = "StockItem";

    private final OutboxJpaRepository repo;
    private final InventoryTopicsProperties topics;
    private final ObjectMapper mapper;

    OutboxAdapter(OutboxJpaRepository repo, InventoryTopicsProperties topics, ObjectMapper mapper) {
        this.repo = repo;
        this.topics = topics;
        this.mapper = mapper;
    }

    @Override
    public void registerStockReserved(String orderId) {
        repo.save(entry(orderId, "StockReserved", new StockReservedMessage(orderId)));
    }

    @Override
    public void registerStockUnavailable(String orderId, String reason) {
        repo.save(entry(orderId, "StockUnavailable", new StockUnavailableMessage(orderId, reason)));
    }

    @Override
    public void registerReservationReleased(String orderId) {
        repo.save(entry(orderId, "ReservationReleased", new ReservationReleasedMessage(orderId)));
    }

    private OutboxJpaEntity entry(String orderId, String eventType, Object payload) {
        return new OutboxJpaEntity(
                UUID.randomUUID(),   // id = messageId usado para dedup no consumer do Orders
                AGGREGATE_TYPE,
                orderId,
                topics.inventoryReplies(),
                orderId,             // msgKey = orderId garante ordem por partição
                eventType,
                serialize(payload),
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
