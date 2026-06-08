package com.example.inventory.infrastructure.messaging.in;

import com.example.inventory.domain.port.in.ReleaseReservationUseCase;
import com.example.inventory.domain.port.in.ReserveStockUseCase;
import com.example.inventory.infrastructure.messaging.InvalidCommandException;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;

/**
 * Adapter de entrada: consome comandos do orquestrador e executa os casos de uso.
 *
 * <p>Idempotente (dedup via inbox) e com commit manual. A dedup + execução do caso de uso
 * + persistência do outbox ocorrem na MESMA transação; o ack ao Kafka só vem depois.
 * Se o ack falhar após o commit, a mensagem reaparece e a dedup a descarta.
 */
@Component
class InventoryCommandListener {

    private static final Logger log = LoggerFactory.getLogger(InventoryCommandListener.class);

    private final ReserveStockUseCase reserveStockUseCase;
    private final ReleaseReservationUseCase releaseReservationUseCase;
    private final InboxStore inbox;
    private final InventoryCommandMapper mapper;

    InventoryCommandListener(ReserveStockUseCase reserveStockUseCase,
                             ReleaseReservationUseCase releaseReservationUseCase,
                             InboxStore inbox,
                             InventoryCommandMapper mapper) {
        this.reserveStockUseCase = reserveStockUseCase;
        this.releaseReservationUseCase = releaseReservationUseCase;
        this.inbox = inbox;
        this.mapper = mapper;
    }

    @KafkaListener(
            topics = "${inventory.topics.inventory-commands}",
            groupId = "inventory-service")
    @Transactional
    public void onCommand(ConsumerRecord<String, String> record, Acknowledgment ack) {
        String messageId = extractMessageId(record);

        if (inbox.alreadyProcessed(messageId)) {
            log.debug("mensagem duplicada ignorada: messageId={}", messageId);
            ack.acknowledge();
            return;
        }

        InventoryCommandMapper.CommandType type = mapper.resolveType(record);

        switch (type) {
            case RESERVE_STOCK -> reserveStockUseCase.execute(mapper.toReserveCommand(record));
            case RELEASE_RESERVATION -> releaseReservationUseCase.execute(mapper.toReleaseCommand(record));
        }

        inbox.markProcessed(messageId);
        ack.acknowledge();
    }

    private String extractMessageId(ConsumerRecord<String, String> record) {
        var h = record.headers().lastHeader("messageId");
        if (h == null) {
            throw new InvalidCommandException("header messageId ausente");
        }
        return new String(h.value(), StandardCharsets.UTF_8);
    }
}
