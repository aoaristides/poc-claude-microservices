package com.example.payment.application.service;

import com.example.payment.application.command.AuthorizePaymentCommand;
import com.example.payment.domain.model.AuthorizationResult;
import com.example.payment.domain.model.Money;
import com.example.payment.domain.model.Payment;
import com.example.payment.domain.model.PaymentAuthorizationPolicy;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Testes do application service de autorização.
 * Mocks das portas; sem Spring.
 */
@ExtendWith(MockitoExtension.class)
class AuthorizePaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private OutboxPort outbox;

    @Mock
    private PaymentAuthorizationPolicy authorizationPolicy;

    private AuthorizePaymentService service;

    @BeforeEach
    void setUp() {
        service = new AuthorizePaymentService(paymentRepository, outbox, authorizationPolicy);
    }

    @Test
    @DisplayName("AuthorizePayment aprovado deve salvar Payment AUTHORIZED e publicar PaymentAuthorized no outbox")
    void shouldAuthorizeAndPublishWhenApproved() {
        AuthorizePaymentCommand command = new AuthorizePaymentCommand("order-1", new BigDecimal("100.00"), "BRL");
        when(paymentRepository.findByOrderId("order-1")).thenReturn(Optional.empty());
        when(authorizationPolicy.evaluate(any(Money.class))).thenReturn(new AuthorizationResult.Approved());

        service.execute(command);

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(PaymentStatus.AUTHORIZED);

        verify(outbox).registerAuthorized("order-1");
        verify(outbox, never()).registerDeclined(anyString(), anyString());
        verify(outbox, never()).registerRefunded(anyString());
    }

    @Test
    @DisplayName("AuthorizePayment recusado deve salvar Payment DECLINED e publicar PaymentDeclined no outbox")
    void shouldDeclineAndPublishWhenRejected() {
        AuthorizePaymentCommand command = new AuthorizePaymentCommand("order-2", new BigDecimal("99999.00"), "BRL");
        when(paymentRepository.findByOrderId("order-2")).thenReturn(Optional.empty());
        when(authorizationPolicy.evaluate(any(Money.class)))
                .thenReturn(new AuthorizationResult.Declined("amount exceeds limit of 10000"));

        service.execute(command);

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(PaymentStatus.DECLINED);
        assertThat(captor.getValue().getReason()).isEqualTo("amount exceeds limit of 10000");

        verify(outbox).registerDeclined(eq("order-2"), eq("amount exceeds limit of 10000"));
        verify(outbox, never()).registerAuthorized(anyString());
    }

    @Test
    @DisplayName("AuthorizePayment repetido (idempotência) deve re-publicar resultado anterior sem reavaliar política")
    void shouldReplyExistingWithoutReEvaluatingPolicy() {
        AuthorizePaymentCommand command = new AuthorizePaymentCommand("order-3", new BigDecimal("100.00"), "BRL");
        Payment existing = Payment.authorize("order-3", Money.of(new BigDecimal("100.00"), "BRL"));
        when(paymentRepository.findByOrderId("order-3")).thenReturn(Optional.of(existing));

        service.execute(command);

        // Política não deve ser avaliada de novo.
        verify(authorizationPolicy, never()).evaluate(any());
        // Não deve salvar de novo.
        verify(paymentRepository, never()).save(any());
        // Deve re-publicar o resultado existente.
        verify(outbox).registerAuthorized("order-3");
    }

    @Test
    @DisplayName("AuthorizePayment repetido de DECLINED deve re-publicar PaymentDeclined")
    void shouldReplyExistingDeclinedWithoutReEvaluatingPolicy() {
        AuthorizePaymentCommand command = new AuthorizePaymentCommand("order-4", new BigDecimal("100.00"), "BRL");
        Payment existing = Payment.decline("order-4", Money.of(new BigDecimal("100.00"), "BRL"), "unsupported currency: USD");
        when(paymentRepository.findByOrderId("order-4")).thenReturn(Optional.of(existing));

        service.execute(command);

        verify(authorizationPolicy, never()).evaluate(any());
        verify(paymentRepository, never()).save(any());
        verify(outbox).registerDeclined("order-4", "unsupported currency: USD");
        verifyNoMoreInteractions(outbox);
    }
}
