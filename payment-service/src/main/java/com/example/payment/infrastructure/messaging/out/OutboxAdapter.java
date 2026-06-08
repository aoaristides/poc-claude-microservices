package com.example.payment.infrastructure.messaging.out;

import com.example.payment.domain.port.out.OutboxPort;
import com.example.payment.infrastructure.config.PaymentTopicsProperties;
import com.example.payment.infrastructure.messaging.EventSerializationException;
import com.example.payment.infrastructure.messaging.contract.PaymentReplyMessage;
import com.example.payment.infrastructure.persistence.OutboxJpaEntity;
import com.example.payment.infrastructure.persistence.OutboxJpaRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/**
 * Adapter de saída do Outbox.
 *
 * <p>Traduz o evento de domínio para o contrato publicado (Published Language),
 * serializa e grava na tabela outbox — dentro da transação do caller.
 * A publicação real no Kafka fica para o {@link OutboxRelay}.
 */
@Component
class OutboxAdapter implements OutboxPort {

    private static final String AGGREGATE_TYPE = "Payment";

    private final OutboxJpaRepository repo;
    private final PaymentTopicsProperties topics;
    private final ObjectMapper mapper;

    OutboxAdapter(OutboxJpaRepository repo, PaymentTopicsProperties topics, ObjectMapper mapper) {
        this.repo = repo;
        this.topics = topics;
        this.mapper = mapper;
    }

    @Override
    public void registerAuthorized(String orderId) {
        persist(orderId, "PaymentAuthorized", new PaymentReplyMessage(orderId));
    }

    @Override
    public void registerDeclined(String orderId, String reason) {
        persist(orderId, "PaymentDeclined", new PaymentReplyMessage(orderId, reason));
    }

    @Override
    public void registerRefunded(String orderId) {
        persist(orderId, "PaymentRefunded", new PaymentReplyMessage(orderId));
    }

    private void persist(String orderId, String eventType, Object payload) {
        repo.save(new OutboxJpaEntity(
                UUID.randomUUID(),   // id = messageId para dedup no consumer
                AGGREGATE_TYPE,
                orderId,
                topics.paymentReplies(),
                orderId,             // msgKey = orderId garante ordem por partição
                eventType,
                serialize(payload),
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
