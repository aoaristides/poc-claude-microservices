package com.example.payment.infrastructure.config;

import com.example.payment.domain.model.PaymentAuthorizationPolicy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Registra beans de domínio que precisam de configuração injetada. */
@Configuration
public class DomainConfig {

    @Bean
    public PaymentAuthorizationPolicy paymentAuthorizationPolicy(
            PaymentAuthorizationProperties props) {
        return new PaymentAuthorizationPolicy(props.maxAmount());
    }
}
