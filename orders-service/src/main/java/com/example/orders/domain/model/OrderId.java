package com.example.orders.domain.model;

import java.util.Objects;
import java.util.UUID;

/** Identidade do aggregate Order. Value Object envolvendo um UUID. */
public record OrderId(UUID value) {

    public OrderId {
        Objects.requireNonNull(value, "value");
    }

    public static OrderId generate() {
        return new OrderId(UUID.randomUUID());
    }

    public static OrderId of(String value) {
        return new OrderId(UUID.fromString(value));
    }

    public String asString() {
        return value.toString();
    }
}
