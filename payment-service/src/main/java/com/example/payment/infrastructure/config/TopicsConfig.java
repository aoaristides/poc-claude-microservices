package com.example.payment.infrastructure.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Criação dos tópicos no startup (idempotente: o Kafka ignora se já existem).
 *
 * <p>[suposição] 6 partições, replicação 1 para DEV.
 * Em produção: 12+ partições, replicação 3, min.insync.replicas=2.
 */
@Configuration
public class TopicsConfig {

    private static final int PARTITIONS = 6;
    private static final short REPLICAS = 1;

    private final PaymentTopicsProperties topics;

    public TopicsConfig(PaymentTopicsProperties topics) {
        this.topics = topics;
    }

    private NewTopic topic(String name) {
        return TopicBuilder.name(name).partitions(PARTITIONS).replicas(REPLICAS).build();
    }

    @Bean
    NewTopic paymentCommandsTopic() {
        return topic(topics.paymentCommands());
    }

    @Bean
    NewTopic paymentRepliesTopic() {
        return topic(topics.paymentReplies());
    }
}
