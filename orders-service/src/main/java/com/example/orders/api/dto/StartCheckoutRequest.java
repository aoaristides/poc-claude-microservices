package com.example.orders.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

/** DTO de entrada do checkout. Nunca expõe entidade de domínio. */
public record StartCheckoutRequest(
        @NotNull UUID clientId,
        @NotEmpty @Valid List<ItemRequest> items
) {
}
