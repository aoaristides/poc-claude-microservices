package com.example.inventory.infrastructure.messaging.in;

import com.example.inventory.infrastructure.persistence.InboxJpaEntity;
import com.example.inventory.infrastructure.persistence.InboxJpaRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;

/** Fachada de deduplicação: já vimos esta mensagem? Registra a que processamos. */
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
