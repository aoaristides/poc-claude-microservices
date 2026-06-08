package com.example.payment.domain.model;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Objects;

/**
 * Value Object monetário. Imutável, validado no construtor compacto.
 * Espelha o Money do orders-service para manter consistência de linguagem.
 */
public record Money(BigDecimal amount, Currency currency) {

    public Money {
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(currency, "currency");
        if (amount.signum() < 0) {
            throw new IllegalArgumentException("amount não pode ser negativo");
        }
        if (amount.scale() > currency.getDefaultFractionDigits()) {
            throw new IllegalArgumentException("escala incompatível com a moeda");
        }
    }

    public static Money of(BigDecimal amount, String currencyCode) {
        return new Money(amount, Currency.getInstance(currencyCode));
    }

    /** Compara valor ignorando escala (99.8 == 99.80). */
    public boolean isGreaterThan(BigDecimal other) {
        return amount.compareTo(other) > 0;
    }
}
