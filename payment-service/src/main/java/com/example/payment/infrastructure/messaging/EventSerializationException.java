package com.example.payment.infrastructure.messaging;

/** Lançada quando a serialização de um evento para o outbox falha. */
public class EventSerializationException extends RuntimeException {

    public EventSerializationException(String message, Throwable cause) {
        super(message, cause);
    }
}
