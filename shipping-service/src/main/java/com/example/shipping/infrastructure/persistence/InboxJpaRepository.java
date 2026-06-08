package com.example.shipping.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface InboxJpaRepository extends JpaRepository<InboxJpaEntity, String> {
}
