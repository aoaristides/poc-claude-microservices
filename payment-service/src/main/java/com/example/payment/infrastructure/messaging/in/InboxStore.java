package com.example.payment.infrastructure.messaging.in;

import com.example.payment.infrastructure.persistence.InboxJpaEntity;
import com.example.payment.infrastructure.persistence.InboxJpaRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;

/** Fachada de deduplicação: já vimos esta mensagem? registra a que processamos. */
@Component
class InboxStore {

    private final InboxJpaRepository repo;

    InboxStore(InboxJpaRepository repo) {
        this.repo = repo;
    }

    boolean alreadyProcessed(String messageId) {
        return repo.existsById(messageId);
    }

    void markProcessed(String messageId) {
        repo.save(new InboxJpaEntity(messageId, Instant.now()));
    }
}
