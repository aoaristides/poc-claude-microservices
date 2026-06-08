package com.example.inventory.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** Linha da tabela outbox. O id é usado também como messageId (dedup no consumer do Orders). */
@Entity
@Table(name = "outbox")
public class OutboxJpaEntity {

    @Id
    private UUID id;

    @Column(name = "aggregate_type", nullable = false)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false)
    private String aggregateId;

    @Column(nullable = false)
    private String topic;

    @Column(name = "msg_key", nullable = false)
    private String msgKey;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(nullable = false)
    private String payload;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    protected OutboxJpaEntity() {
    }

    public OutboxJpaEntity(UUID id, String aggregateType, String aggregateId,
                           String topic, String msgKey, String eventType,
                           String payload, Instant occurredAt) {
        this.id = id;
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.topic = topic;
        this.msgKey = msgKey;
        this.eventType = eventType;
        this.payload = payload;
        this.occurredAt = occurredAt;
    }

    public UUID getId() {
        return id;
    }

    public String getTopic() {
        return topic;
    }

    public String getMsgKey() {
        return msgKey;
    }

    public String getEventType() {
        return eventType;
    }

    public String getPayload() {
        return payload;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public void markPublished(Instant when) {
        this.publishedAt = when;
    }
}
