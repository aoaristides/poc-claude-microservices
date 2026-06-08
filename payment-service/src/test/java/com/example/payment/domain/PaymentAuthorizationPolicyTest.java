package com.example.payment.domain;

import com.example.payment.domain.model.AuthorizationResult;
import com.example.payment.domain.model.Money;
import com.example.payment.domain.model.PaymentAuthorizationPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testes de domínio puro da política de autorização.
 * Sem Spring, sem mocks — apenas lógica de domínio.
 */
class PaymentAuthorizationPolicyTest {

    private PaymentAuthorizationPolicy policy;

    @BeforeEach
    void setUp() {
        policy = new PaymentAuthorizationPolicy(new BigDecimal("10000"));
    }

    @Test
    @DisplayName("deve aprovar quando amount está abaixo do limite e moeda é BRL")
    void shouldApproveWhenAmountBelowLimit() {
        Money money = Money.of(new BigDecimal("99.80"), "BRL");

        AuthorizationResult result = policy.evaluate(money);

        assertThat(result).isInstanceOf(AuthorizationResult.Approved.class);
    }

    @Test
    @DisplayName("deve aprovar quando amount é exatamente igual ao limite")
    void shouldApproveWhenAmountEqualsLimit() {
        Money money = Money.of(new BigDecimal("10000"), "BRL");

        AuthorizationResult result = policy.evaluate(money);

        assertThat(result).isInstanceOf(AuthorizationResult.Approved.class);
    }

    @Test
    @DisplayName("deve recusar quando amount excede o limite")
    void shouldDeclineWhenAmountExceedsLimit() {
        Money money = Money.of(new BigDecimal("10000.01"), "BRL");

        AuthorizationResult result = policy.evaluate(money);

        assertThat(result).isInstanceOf(AuthorizationResult.Declined.class);
        AuthorizationResult.Declined declined = (AuthorizationResult.Declined) result;
        assertThat(declined.reason()).contains("exceeds limit");
    }

    @Test
    @DisplayName("deve recusar quando moeda não é suportada")
    void shouldDeclineWhenCurrencyNotSupported() {
        Money money = Money.of(new BigDecimal("100.00"), "USD");

        AuthorizationResult result = policy.evaluate(money);

        assertThat(result).isInstanceOf(AuthorizationResult.Declined.class);
        AuthorizationResult.Declined declined = (AuthorizationResult.Declined) result;
        assertThat(declined.reason()).contains("unsupported currency");
    }

    @Test
    @DisplayName("deve lançar exceção ao criar política com maxAmount inválido")
    void shouldThrowWhenMaxAmountIsInvalid() {
        assertThatThrownBy(() -> new PaymentAuthorizationPolicy(BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new PaymentAuthorizationPolicy(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
