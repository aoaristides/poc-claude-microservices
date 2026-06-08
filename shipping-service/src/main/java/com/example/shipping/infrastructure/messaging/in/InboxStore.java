package com.example.shipping.infrastructure.messaging.in;

import com.example.shipping.infrastructure.persistence.InboxJpaEntity;
import com.example.shipping.infrastructure.persistence.InboxJpaRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;

/** Fachada de deduplicação: verifica e registra mensagens já processadas. */
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
