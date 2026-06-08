package com.example.payment.application.service;

import com.example.payment.application.command.RefundPaymentCommand;
import com.example.payment.domain.exception.InvalidPaymentTransitionException;
import com.example.payment.domain.exception.PaymentNotFoundException;
import com.example.payment.domain.model.Money;
import com.example.payment.domain.model.Payment;
import com.example.payment.domain.model.PaymentStatus;
import com.example.payment.domain.port.out.OutboxPort;
import com.example.payment.domain.port.out.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testes do application service de estorno.
 * Mocks das portas; sem Spring.
 */
@ExtendWith(MockitoExtension.class)
class RefundPaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private OutboxPort outbox;

    private RefundPaymentService service;

    @BeforeEach
    void setUp() {
        service = new RefundPaymentService(paymentRepository, outbox);
    }

    @Test
    @DisplayName("RefundPayment de AUTHORIZED deve transicionar para REFUNDED e publicar PaymentRefunded")
    void shouldRefundAuthorizedPaymentAndPublish() {
        Payment authorized = Payment.authorize("order-1", Money.of(new BigDecimal("100.00"), "BRL"));
        when(paymentRepository.findByOrderId("order-1")).thenReturn(Optional.of(authorized));

        service.execute(new RefundPaymentCommand("order-1"));

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(PaymentStatus.REFUNDED);

        verify(outbox).registerRefunded("order-1");
    }

    @Test
    @DisplayName("RefundPayment de REFUNDED deve ser idempotente e re-publicar PaymentRefunded")
    void shouldBeIdempotentWhenAlreadyRefunded() {
        Payment refunded = Payment.authorize("order-2", Money.of(new BigDecimal("50.00"), "BRL"));
        refunded.refund();
        when(paymentRepository.findByOrderId("order-2")).thenReturn(Optional.of(refunded));

        // Não deve lançar exceção
        service.execute(new RefundPaymentCommand("order-2"));

        verify(outbox).registerRefunded("order-2");
    }

    @Test
    @DisplayName("RefundPayment quando orderId não existe deve lançar PaymentNotFoundException")
    void shouldThrowWhenPaymentNotFound() {
        when(paymentRepository.findByOrderId("order-99")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(new RefundPaymentCommand("order-99")))
                .isInstanceOf(PaymentNotFoundException.class)
                .hasMessageContaining("order-99");
    }

    @Test
    @DisplayName("RefundPayment de DECLINED deve lançar InvalidPaymentTransitionException")
    void shouldThrowWhenRefundingDeclinedPayment() {
        Payment declined = Payment.decline("order-3", Money.of(new BigDecimal("100.00"), "BRL"), "unsupported currency");
        when(paymentRepository.findByOrderId("order-3")).thenReturn(Optional.of(declined));

        assertThatThrownBy(() -> service.execute(new RefundPaymentCommand("order-3")))
                .isInstanceOf(InvalidPaymentTransitionException.class);
    }
}
