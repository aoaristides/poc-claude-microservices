package com.example.orders.infrastructure.messaging.out;

import com.example.orders.infrastructure.persistence.OutboxJpaEntity;
import com.example.orders.infrastructure.persistence.OutboxJpaRepository;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Relay do Outbox: lê mensagens pendentes e publica no Kafka.
 *
 * <p>Roda em loop curto ({@code @Scheduled}). Publica de forma síncrona e só então
 * marca como publicado: se o envio falhar, a linha continua pendente e é retentada
 * no próximo ciclo. Combinado com a dedup do consumer (inbox), dá at-least-once seguro.
 *
 * <p>[inferência] Em volume maior, troque o polling por CDC (Debezium) para menor latência.
 */
@Component
class OutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);
    private static final int BATCH_SIZE = 100;

    private final OutboxJpaRepository repo;
    private final KafkaTemplate<String, String> kafka;

    OutboxRelay(OutboxJpaRepository repo, KafkaTemplate<String, String> kafka) {
        this.repo = repo;
        this.kafka = kafka;
    }

    @Scheduled(fixedDelayString = "${orders.outbox.relay-delay-ms:500}")
    @Transactional
    public void publishPending() {
        List<OutboxJpaEntity> pending = repo.findPending(PageRequest.of(0, BATCH_SIZE));
        for (OutboxJpaEntity entry : pending) {
            try {
                send(entry);
                entry.markPublished(Instant.now());   // entidade gerenciada: flush no commit
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("relay interrompido ao publicar outbox id={}", entry.getId());
                break;
            } catch (ExecutionException e) {
                // não marca como publicado: será retentado no próximo ciclo
                log.error("falha publicando outbox id={} topic={}", entry.getId(), entry.getTopic(), e);
            }
        }
    }

    private void send(OutboxJpaEntity entry) throws InterruptedException, ExecutionException {
        var record = new ProducerRecord<>(entry.getTopic(), entry.getMsgKey(), entry.getPayload());
        record.headers().add("messageId", entry.getId().toString().getBytes(StandardCharsets.UTF_8));
        record.headers().add("eventType", entry.getEventType().getBytes(StandardCharsets.UTF_8));
        record.headers().add("schemaVersion", "v1".getBytes(StandardCharsets.UTF_8));
        // envio síncrono: garante sucesso antes de marcar publicado
        kafka.send(record).get();
    }
}
