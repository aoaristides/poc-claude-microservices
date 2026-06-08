package com.example.shipping.infrastructure.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Criação dos tópicos no startup (idempotente: Kafka ignora se já existem).
 *
 * <p>[suposição] 6 partições e replicação 1 para DEV. Em produção: 12+ partições e
 * replicação 3 com {@code min.insync.replicas=2}.
 */
@Configuration
public class TopicsConfig {

    private static final int PARTITIONS = 6;
    private static final short REPLICAS = 1;

    private final ShippingTopicsProperties topics;

    public TopicsConfig(ShippingTopicsProperties topics) {
        this.topics = topics;
    }

    private NewTopic topic(String name) {
        return TopicBuilder.name(name).partitions(PARTITIONS).replicas(REPLICAS).build();
    }

    @Bean
    NewTopic shippingCommandsTopic() {
        return topic(topics.commands());
    }

    @Bean
    NewTopic shippingRepliesTopic() {
        return topic(topics.replies());
    }
}
