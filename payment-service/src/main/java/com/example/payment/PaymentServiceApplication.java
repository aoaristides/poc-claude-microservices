package com.example.payment;

import com.example.payment.infrastructure.config.PaymentAuthorizationProperties;
import com.example.payment.infrastructure.config.PaymentOutboxProperties;
import com.example.payment.infrastructure.config.PaymentTopicsProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties({
        PaymentTopicsProperties.class,
        PaymentAuthorizationProperties.class,
        PaymentOutboxProperties.class
})
public class PaymentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaymentServiceApplication.class, args);
    }
}
