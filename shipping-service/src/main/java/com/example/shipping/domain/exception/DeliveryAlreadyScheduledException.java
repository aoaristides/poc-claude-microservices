package com.example.shipping.domain.exception;

/** Lançada quando se tenta agendar uma entrega que já foi processada. */
public class DeliveryAlreadyScheduledException extends RuntimeException {

    public DeliveryAlreadyScheduledException(String message) {
        super(message);
    }
}
