package com.example.shipping.infrastructure.messaging;

/** Lançada quando a serialização de um payload do outbox falha. */
public class EventSerializationException extends RuntimeException {

    public EventSerializationException(String message, Throwable cause) {
        super(message, cause);
    }
}
