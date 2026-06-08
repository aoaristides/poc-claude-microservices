package com.example.inventory;

import com.example.inventory.infrastructure.config.InventoryOutboxProperties;
import com.example.inventory.infrastructure.config.InventoryTopicsProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Serviço de Estoque — participante da saga de checkout.
 *
 * <p>{@code @EnableScheduling} liga o OutboxRelay;
 * {@code @EnableConfigurationProperties} registra os nomes de tópicos e
 * configurações do outbox vindos do application.yml.
 */
@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties({InventoryTopicsProperties.class, InventoryOutboxProperties.class})
public class InventoryServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(InventoryServiceApplication.class, args);
    }
}
