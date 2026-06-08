package com.example.shipping.infrastructure.messaging.in;

import com.example.shipping.domain.port.in.ScheduleDeliveryUseCase;
import com.example.shipping.infrastructure.messaging.InvalidEventException;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;

/**
 * Adapter de entrada: consome comandos de agendamento de entrega.
 *
 * <p>Idempotente via inbox (dedup por messageId). Tudo (inbox + delivery + outbox)
 * persiste na MESMA transação; ack ao Kafka só ocorre depois do commit.
 * Se o ack falhar após commit, a mensagem reaparece e a dedup a descarta.
 */
@Component
class ShippingCommandListener {

    private static final Logger log = LoggerFactory.getLogger(ShippingCommandListener.class);

    private final ScheduleDeliveryUseCase useCase;
    private final InboxStore inbox;
    private final ShippingCommandMapper mapper;

    ShippingCommandListener(ScheduleDeliveryUseCase useCase,
                            InboxStore inbox,
                            ShippingCommandMapper mapper) {
        this.useCase = useCase;
        this.inbox = inbox;
        this.mapper = mapper;
    }

    @KafkaListener(topics = "${shipping.topics.commands}", groupId = "shipping-service")
    @Transactional
    public void onCommand(ConsumerRecord<String, String> record, Acknowledgment ack) {
        String messageId = messageId(record);

        if (inbox.alreadyProcessed(messageId)) {
            log.debug("mensagem duplicada ignorada: messageId={}", messageId);
            ack.acknowledge();
            return;
        }

        ScheduleDeliveryCommand command = mapper.toCommand(record);
        useCase.execute(command.orderId(), command.skus());
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
