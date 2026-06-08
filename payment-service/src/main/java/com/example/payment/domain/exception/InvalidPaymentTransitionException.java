package com.example.payment.domain.exception;

/** Lançada quando uma transição de estado inválida é tentada no aggregate Payment. */
public class InvalidPaymentTransitionException extends RuntimeException {

    public InvalidPaymentTransitionException(String message) {
        super(message);
    }
}
