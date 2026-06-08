package com.example.inventory.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/** Registro de mensagem já processada (deduplicação de consumer). */
@Entity
@Table(name = "inbox_message")
public class InboxJpaEntity {

    @Id
    @Column(name = "message_id")
    private String messageId;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;

    protected InboxJpaEntity() {
    }

    public InboxJpaEntity(String messageId, Instant processedAt) {
        this.messageId = messageId;
        this.processedAt = processedAt;
    }

    public String getMessageId() {
        return messageId;
    }
}
