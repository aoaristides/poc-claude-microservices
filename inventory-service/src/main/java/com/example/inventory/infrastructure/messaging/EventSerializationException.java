package com.example.inventory.infrastructure.messaging;

/** Falha ao serializar payload para o outbox. */
public class EventSerializationException extends RuntimeException {

    public EventSerializationException(String message, Throwable cause) {
        super(message, cause);
    }
}
