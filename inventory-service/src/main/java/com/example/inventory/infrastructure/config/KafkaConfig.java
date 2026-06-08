package com.example.inventory.infrastructure.config;

import com.example.inventory.infrastructure.messaging.InvalidCommandException;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.ExponentialBackOff;

/**
 * Configuração de tratamento de erro do consumer.
 *
 * <p>Retenta com backoff exponencial; após esgotar, publica no tópico {@code *.DLT}
 * (Dead Letter Topic). Payload inválido / eventType desconhecido NÃO são retentados —
 * vão direto à DLT.
 */
@Configuration
public class KafkaConfig {

    private static final Logger log = LoggerFactory.getLogger(KafkaConfig.class);

    @Bean
    public DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<String, String> template) {
        // Publica a mensagem falha em <topic>.DLT preservando a chave.
        var recoverer = new DeadLetterPublishingRecoverer(template);

        // 1s, 2s, 4s, 8s... com teto de 10s. Mantém o consumer vivo sem loop apertado.
        var backOff = new ExponentialBackOff(1000L, 2.0);
        backOff.setMaxInterval(10_000L);
        backOff.setMaxElapsedTime(60_000L);

        var handler = new DefaultErrorHandler(recoverer, backOff);
        // Payload corrompido/tipo desconhecido não adianta retentar.
        handler.addNotRetryableExceptions(InvalidCommandException.class);
        handler.setRetryListeners((ConsumerRecord<?, ?> rec, Exception ex, int attempt) ->
                log.warn("retry {} para topic={} offset={} causa={}",
                        attempt, rec.topic(), rec.offset(), ex.getMessage()));
        return handler;
    }
}
