package com.example.orders.infrastructure.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Criação dos tópicos no startup (idempotente: o Kafka ignora se já existem).
 *
 * <p>[suposição] 6 partições e replicação 1 para DEV. Em produção: 12+ partições e
 * replicação 3 com {@code min.insync.replicas=2}. Aumentar partição depois não
 * preserva a ordem antiga — dimensione para crescimento.
 */
@Configuration
public class TopicsConfig {

    private static final int PARTITIONS = 6;
    private static final short REPLICAS = 1;

    private final OrdersTopicsProperties topics;

    public TopicsConfig(OrdersTopicsProperties topics) {
        this.topics = topics;
    }

    private NewTopic topic(String name) {
        return TopicBuilder.name(name).partitions(PARTITIONS).replicas(REPLICAS).build();
    }

    @Bean
    NewTopic inventoryCommandsTopic() {
        return topic(topics.inventoryCommands());
    }

    @Bean
    NewTopic paymentCommandsTopic() {
        return topic(topics.paymentCommands());
    }

    @Bean
    NewTopic shippingCommandsTopic() {
        return topic(topics.shippingCommands());
    }

    @Bean
    NewTopic orderEventsTopic() {
        return topic(topics.orderEvents());
    }

    @Bean
    NewTopic inventoryRepliesTopic() {
        return topic(topics.inventoryReplies());
    }

    @Bean
    NewTopic paymentRepliesTopic() {
        return topic(topics.paymentReplies());
    }

    @Bean
    NewTopic shippingRepliesTopic() {
        return topic(topics.shippingReplies());
    }
}
