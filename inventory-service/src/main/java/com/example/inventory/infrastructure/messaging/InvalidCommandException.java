package com.example.inventory.infrastructure.messaging;

/**
 * Exceção de infraestrutura: mensagem inválida (payload malformado ou eventType desconhecido).
 * Marcada como não-retryable no {@link com.example.inventory.infrastructure.config.KafkaConfig}.
 */
public class InvalidCommandException extends RuntimeException {

    public InvalidCommandException(String message) {
        super(message);
    }

    public InvalidCommandException(String message, Throwable cause) {
        super(message, cause);
    }
}
