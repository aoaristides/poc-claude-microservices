package com.example.inventory.infrastructure.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Criação dos tópicos no startup (idempotente: o Kafka ignora se já existem).
 *
 * <p>[suposição] 6 partições e replicação 1 para DEV. Em produção: 12+ partições e
 * replicação 3 com {@code min.insync.replicas=2}.
 */
@Configuration
public class TopicsConfig {

    private static final int PARTITIONS = 6;
    private static final short REPLICAS = 1;

    private final InventoryTopicsProperties topics;

    public TopicsConfig(InventoryTopicsProperties topics) {
        this.topics = topics;
    }

    @Bean
    NewTopic inventoryCommandsTopic() {
        return TopicBuilder.name(topics.inventoryCommands())
                .partitions(PARTITIONS)
                .replicas(REPLICAS)
                .build();
    }

    @Bean
    NewTopic inventoryRepliesTopic() {
        return TopicBuilder.name(topics.inventoryReplies())
                .partitions(PARTITIONS)
                .replicas(REPLICAS)
                .build();
    }
}
