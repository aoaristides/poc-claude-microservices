package com.example.shipping.infrastructure.config;

import com.example.shipping.infrastructure.messaging.InvalidEventException;
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
 * <p>Retenta com backoff exponencial; após esgotar, publica no tópico {@code *.DLT}.
 * Erros de payload inválido ({@link InvalidEventException}) NÃO são retentados.
 */
@Configuration
public class KafkaConfig {

    private static final Logger log = LoggerFactory.getLogger(KafkaConfig.class);

    @Bean
    public DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<String, String> template) {
        var recoverer = new DeadLetterPublishingRecoverer(template);

        // 1s, 2s, 4s, 8s... com teto em 10s. Teto total 60s.
        var backOff = new ExponentialBackOff(1000L, 2.0);
        backOff.setMaxInterval(10_000L);
        backOff.setMaxElapsedTime(60_000L);

        var handler = new DefaultErrorHandler(recoverer, backOff);
        // Payload corrompido/tipo desconhecido não melhora com retry.
        handler.addNotRetryableExceptions(InvalidEventException.class);
        handler.setRetryListeners((ConsumerRecord<?, ?> rec, Exception ex, int attempt) ->
                log.warn("retry {} para topic={} offset={} causa={}",
                        attempt, rec.topic(), rec.offset(), ex.getMessage()));
        return handler;
    }
}
