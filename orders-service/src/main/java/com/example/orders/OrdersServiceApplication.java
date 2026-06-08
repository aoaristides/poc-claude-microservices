package com.example.orders;

import com.example.orders.infrastructure.config.OrdersSagaProperties;
import com.example.orders.infrastructure.config.OrdersTopicsProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Serviço de Pedidos — orquestrador da saga de checkout.
 *
 * <p>{@code @EnableScheduling} liga o OutboxRelay; {@code @EnableConfigurationProperties}
 * registra os nomes de tópicos vindos do application.yml.
 */
@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties({OrdersTopicsProperties.class, OrdersSagaProperties.class})
public class OrdersServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrdersServiceApplication.class, args);
    }
}
