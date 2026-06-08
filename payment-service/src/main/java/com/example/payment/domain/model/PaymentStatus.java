package com.example.payment.domain.model;

/** Estados possíveis de um Payment. Transições protegidas no aggregate. */
public enum PaymentStatus {
    AUTHORIZED,
    DECLINED,
    REFUNDED
}
