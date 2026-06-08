package com.example.payment.domain;

import com.example.payment.domain.exception.InvalidPaymentTransitionException;
import com.example.payment.domain.model.Money;
import com.example.payment.domain.model.Payment;
import com.example.payment.domain.model.PaymentStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testes de domínio puro do aggregate Payment.
 * Sem Spring, sem mocks — apenas lógica de domínio.
 */
class PaymentTest {

    private static final Money BRL_100 = Money.of(new BigDecimal("100.00"), "BRL");
    private static final String ORDER_ID = "order-123";

    @Test
    @DisplayName("Payment.authorize deve criar Payment com status AUTHORIZED")
    void shouldCreateAuthorizedPayment() {
        Payment payment = Payment.authorize(ORDER_ID, BRL_100);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.AUTHORIZED);
        assertThat(payment.getOrderId()).isEqualTo(ORDER_ID);
        assertThat(payment.getReason()).isNull();
        assertThat(payment.isAuthorized()).isTrue();
    }

    @Test
    @DisplayName("Payment.decline deve criar Payment com status DECLINED e reason")
    void shouldCreateDeclinedPayment() {
        Payment payment = Payment.decline(ORDER_ID, BRL_100, "amount exceeds limit");

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.DECLINED);
        assertThat(payment.getReason()).isEqualTo("amount exceeds limit");
        assertThat(payment.isDeclined()).isTrue();
    }

    @Test
    @DisplayName("refund de AUTHORIZED deve transicionar para REFUNDED")
    void shouldRefundAuthorizedPayment() {
        Payment payment = Payment.authorize(ORDER_ID, BRL_100);

        payment.refund();

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(payment.isRefunded()).isTrue();
    }

    @Test
    @DisplayName("refund de REFUNDED deve ser idempotente (no-op)")
    void shouldBeIdempotentWhenAlreadyRefunded() {
        Payment payment = Payment.authorize(ORDER_ID, BRL_100);
        payment.refund();

        // segunda chamada não deve lançar exceção
        payment.refund();

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
    }

    @Test
    @DisplayName("refund de DECLINED deve lançar InvalidPaymentTransitionException")
    void shouldThrowWhenRefundingDeclinedPayment() {
        Payment payment = Payment.decline(ORDER_ID, BRL_100, "unsupported currency");

        assertThatThrownBy(payment::refund)
                .isInstanceOf(InvalidPaymentTransitionException.class)
                .hasMessageContaining("DECLINED");
    }

    @Test
    @DisplayName("Payment.authorize deve lançar NullPointerException se orderId nulo")
    void shouldThrowWhenOrderIdIsNull() {
        assertThatThrownBy(() -> Payment.authorize(null, BRL_100))
                .isInstanceOf(NullPointerException.class);
    }
}
