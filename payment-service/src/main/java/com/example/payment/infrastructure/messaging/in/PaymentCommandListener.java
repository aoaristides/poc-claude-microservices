package com.example.payment.infrastructure.messaging.in;

import com.example.payment.application.command.AuthorizePaymentCommand;
import com.example.payment.application.command.RefundPaymentCommand;
import com.example.payment.domain.port.in.AuthorizePaymentUseCase;
import com.example.payment.domain.port.in.RefundPaymentUseCase;
import com.example.payment.infrastructure.messaging.InvalidPaymentEventException;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;

/**
 * Adapter de entrada: consome comandos do orquestrador e delega ao application service.
 *
 * <p>Idempotente (dedup via inbox) e com commit manual.
 * A dedup + mudança de estado do Payment + registro no outbox ocorrem na MESMA transação.
 * O ack ao Kafka só acontece depois do commit. Se o ack falhar, a mensagem reaparece
 * e a dedup no inbox a descarta.
 */
@Component
class PaymentCommandListener {

    private static final Logger log = LoggerFactory.getLogger(PaymentCommandListener.class);

    private final AuthorizePaymentUseCase authorizePaymentUseCase;
    private final RefundPaymentUseCase refundPaymentUseCase;
    private final InboxStore inbox;
    private final PaymentCommandMapper mapper;

    PaymentCommandListener(AuthorizePaymentUseCase authorizePaymentUseCase,
                           RefundPaymentUseCase refundPaymentUseCase,
                           InboxStore inbox,
                           PaymentCommandMapper mapper) {
        this.authorizePaymentUseCase = authorizePaymentUseCase;
        this.refundPaymentUseCase = refundPaymentUseCase;
        this.inbox = inbox;
        this.mapper = mapper;
    }

    @KafkaListener(
            topics = "${payment.topics.payment-commands}",
            groupId = "payment-service")
    @Transactional
    public void onCommand(ConsumerRecord<String, String> record, Acknowledgment ack) {
        String messageId = extractMessageId(record);

        if (inbox.alreadyProcessed(messageId)) {
            log.debug("mensagem duplicada ignorada: {}", messageId);
            ack.acknowledge();
            return;
        }

        Object command = mapper.toDomainCommand(record);

        switch (command) {
            case AuthorizePaymentCommand c -> authorizePaymentUseCase.execute(c);
            case RefundPaymentCommand c -> refundPaymentUseCase.execute(c);
            default -> throw new InvalidPaymentEventException(
                    "tipo de comando não reconhecido: " + command.getClass().getName());
        }

        inbox.markProcessed(messageId);
        ack.acknowledge();
    }

    private String extractMessageId(ConsumerRecord<?, ?> record) {
        var h = record.headers().lastHeader("messageId");
        if (h == null) {
            throw new InvalidPaymentEventException("header messageId ausente");
        }
        return new String(h.value(), StandardCharsets.UTF_8);
    }
}
