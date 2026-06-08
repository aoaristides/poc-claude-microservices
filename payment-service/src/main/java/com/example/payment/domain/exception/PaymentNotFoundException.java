package com.example.payment.domain.exception;

/** Lançada quando um Payment não é encontrado para o orderId informado. */
public class PaymentNotFoundException extends RuntimeException {

    public PaymentNotFoundException(String orderId) {
        super("payment não encontrado para orderId: " + orderId);
    }
}
