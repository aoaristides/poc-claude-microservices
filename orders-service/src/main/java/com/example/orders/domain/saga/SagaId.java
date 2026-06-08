package com.example.orders.domain.saga;

import java.util.Objects;
import java.util.UUID;

/** Identidade da saga de checkout. */
public record SagaId(UUID value) {

    public SagaId {
        Objects.requireNonNull(value, "value");
    }

    public static SagaId generate() {
        return new SagaId(UUID.randomUUID());
    }

    public String asString() {
        return value.toString();
    }
}
