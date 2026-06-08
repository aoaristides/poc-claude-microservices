package com.example.inventory.infrastructure.config;

import com.example.inventory.domain.service.StockReservationService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Registra beans de domínio sem anotações de Spring no próprio domínio. */
@Configuration
public class DomainConfig {

    @Bean
    public StockReservationService stockReservationService() {
        return new StockReservationService();
    }
}
