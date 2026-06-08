package com.example.orders.domain.model;

import java.util.Objects;

/** Identificador de produto (Stock Keeping Unit). Value Object com validação simples. */
public record Sku(String value) {

    public Sku {
        Objects.requireNonNull(value, "value");
        if (value.isBlank()) {
            throw new IllegalArgumentException("sku não pode ser vazio");
        }
    }
}
