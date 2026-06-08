package com.example.orders.infrastructure.messaging.in;

import com.example.orders.domain.port.in.ProcessSagaReplyUseCase;
import com.example.orders.domain.saga.SagaReply;
import com.example.orders.infrastructure.messaging.InvalidEventException;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;

/**
 * Adapter de entrada: consome as respostas dos participantes e avança a saga.
 *
 * <p>Idempotente (dedup via inbox) e com commit manual. A dedup + o avanço da saga
 * + o outbox commitam na MESMA transação; o ack ao Kafka só ocorre depois. Se o ack
 * falhar após o commit, a mensagem reaparece e a dedup a descarta.
 */
@Component
class SagaReplyListener {

    private static final Logger log = LoggerFactory.getLogger(SagaReplyListener.class);

    private final ProcessSagaReplyUseCase useCase;
    private final InboxStore inbox;
    private final SagaReplyMapper mapper;

    SagaReplyListener(ProcessSagaReplyUseCase useCase, InboxStore inbox, SagaReplyMapper mapper) {
        this.useCase = useCase;
        this.inbox = inbox;
        this.mapper = mapper;
    }

    @KafkaListener(
            topics = {
                    "${orders.topics.inventory-replies}",
                    "${orders.topics.payment-replies}",
                    "${orders.topics.shipping-replies}"
            },
            groupId = "orders-saga")
    @Transactional
    public void onReply(ConsumerRecord<String, String> record, Acknowledgment ack) {
        String messageId = messageId(record);

        if (inbox.alreadyProcessed(messageId)) {
            log.debug("mensagem duplicada ignorada: {}", messageId);
            ack.acknowledge();
            return;
        }

        SagaReply reply = mapper.toDomain(record);
        useCase.execute(reply);
        inbox.markProcessed(messageId);

        ack.acknowledge();
    }

    private String messageId(ConsumerRecord<String, String> record) {
        var h = record.headers().lastHeader("messageId");
        if (h == null) {
            throw new InvalidEventException("header messageId ausente");
        }
        return new String(h.value(), StandardCharsets.UTF_8);
    }
}
