package com.example.orders.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** DTO de item na requisição de checkout (borda HTTP). */
public record ItemRequest(
        @NotBlank String sku,
        @Positive int quantity,
        @NotNull @Positive BigDecimal unitPrice,
        @NotBlank @Size(min = 3, max = 3) String currency
) {
}
