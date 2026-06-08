package com.example.orders.infrastructure.messaging;

/** Falha técnica ao serializar/desserializar payload de mensagem. */
public class EventSerializationException extends RuntimeException {
    public EventSerializationException(String message, Throwable cause) {
        super(message, cause);
    }
}
