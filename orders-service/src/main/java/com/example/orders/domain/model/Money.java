package com.example.orders.domain.model;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Objects;

/**
 * Value Object monetário. Imutável, validado no construtor compacto.
 * Prefira {@code Money} a {@code BigDecimal} cru: expressa intenção e evita
 * somar moedas diferentes por engano.
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

    /** Soma exigindo mesma moeda — invariante protegida no próprio VO. */
    public Money add(Money other) {
        requireSameCurrency(other);
        return new Money(amount.add(other.amount), currency);
    }

    public Money multiply(int factor) {
        return new Money(amount.multiply(BigDecimal.valueOf(factor)), currency);
    }

    private void requireSameCurrency(Money other) {
        if (!currency.equals(other.currency)) {
            throw new IllegalArgumentException("moedas incompatíveis: %s != %s"
                    .formatted(currency, other.currency));
        }
    }
}
