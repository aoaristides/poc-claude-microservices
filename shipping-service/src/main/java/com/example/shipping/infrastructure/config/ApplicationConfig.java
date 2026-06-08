package com.example.shipping.infrastructure.config;

import com.example.shipping.application.service.ScheduleDeliveryService;
import com.example.shipping.domain.port.in.ScheduleDeliveryUseCase;
import com.example.shipping.domain.port.out.DeliveryRepository;
import com.example.shipping.domain.port.out.OutboxPort;
import com.example.shipping.domain.service.DeliverySchedulingPolicy;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Configuração da camada de aplicação.
 *
 * <p>Instancia o application service e o domain service, injetando as portas e a
 * propriedade {@code simulateFailure}. Centralizar aqui mantém o Spring fora do
 * domínio e da aplicação.
 */
@Configuration
@EnableScheduling
@EnableConfigurationProperties({ShippingTopicsProperties.class, ShippingProperties.class})
public class ApplicationConfig {

    @Bean
    public DeliverySchedulingPolicy deliverySchedulingPolicy(ShippingProperties props) {
        return new DeliverySchedulingPolicy(props.simulateFailure());
    }

    @Bean
    public ScheduleDeliveryUseCase scheduleDeliveryUseCase(DeliveryRepository deliveryRepository,
                                                           OutboxPort outboxPort,
                                                           DeliverySchedulingPolicy policy) {
        return new ScheduleDeliveryService(deliveryRepository, outboxPort, policy);
    }
}
