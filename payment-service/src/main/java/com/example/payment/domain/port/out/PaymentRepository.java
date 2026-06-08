package com.example.payment.domain.port.out;

import com.example.payment.domain.model.Payment;

import java.util.Optional;

/** Porta de saída: persistência do aggregate Payment. */
public interface PaymentRepository {

    void save(Payment payment);

    Optional<Payment> findByOrderId(String orderId);
}
