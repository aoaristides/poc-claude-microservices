package com.example.shipping.infrastructure.messaging.out;

import com.example.shipping.domain.model.Delivery;
import com.example.shipping.domain.model.DeliveryStatus;
import com.example.shipping.domain.port.out.OutboxPort;
import com.example.shipping.infrastructure.config.ShippingTopicsProperties;
import com.example.shipping.infrastructure.messaging.EventSerializationException;
import com.example.shipping.infrastructure.persistence.OutboxJpaEntity;
import com.example.shipping.infrastructure.persistence.OutboxJpaRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/**
 * Adapter de saída do Outbox.
 *
 * <p>Traduz o estado final do aggregate {@link Delivery} para o contrato publicado,
 * resolve o eventType e serializa o payload. Persiste na tabela outbox dentro da
 * transação do caller. A publicação real no Kafka é feita pelo {@link OutboxRelay}.
 */
@Component
class OutboxAdapter implements OutboxPort {

    private static final String AGGREGATE_TYPE = "Delivery";

    private final OutboxJpaRepository repo;
    private final ShippingTopicsProperties topics;
    private final ObjectMapper mapper;

    OutboxAdapter(OutboxJpaRepository repo, ShippingTopicsProperties topics, ObjectMapper mapper) {
        this.repo = repo;
        this.topics = topics;
        this.mapper = mapper;
    }

    @Override
    public void registerReply(Delivery delivery) {
        String orderId = delivery.getOrderId();
        String eventType;
        String payload;

        if (delivery.getStatus() == DeliveryStatus.SCHEDULED) {
            eventType = "DeliveryScheduled";
            payload = serialize(new DeliveryScheduledPayload(orderId));
        } else {
            eventType = "DeliveryFailed";
            payload = serialize(new DeliveryFailedPayload(orderId, delivery.getReason()));
        }

        repo.save(new OutboxJpaEntity(
                UUID.randomUUID(),   // id = messageId usado para dedup no consumer (orders)
                AGGREGATE_TYPE,
                orderId,
                topics.replies(),
                orderId,             // msgKey = orderId garante ordem por partição
                eventType,
                payload,
                Instant.now()));
    }

    private String serialize(Object payload) {
        try {
            return mapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new EventSerializationException("falha serializando payload do outbox", e);
        }
    }
}
