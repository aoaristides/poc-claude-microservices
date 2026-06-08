package com.example.orders.domain.model;

import java.util.Objects;
import java.util.UUID;

/** Referência ao cliente por id (aggregates se referenciam por id, não por objeto). */
public record ClientId(UUID value) {

    public ClientId {
        Objects.requireNonNull(value, "value");
    }

    public static ClientId of(String value) {
        return new ClientId(UUID.fromString(value));
    }

    public String asString() {
        return value.toString();
    }
}
