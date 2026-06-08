package com.example.payment.domain.model;

import com.example.payment.domain.exception.InvalidPaymentTransitionException;

import java.util.Objects;

/**
 * Aggregate root do contexto de pagamento.
 *
 * <p>Mantém invariantes de transição de estado (sem setter público de status).
 * Toda mudança de estado passa por um método de domínio que valida a pré-condição.
 *
 * <p>Idempotência de negócio:
 * <ul>
 *   <li>{@code authorize} em status já final devolve o estado atual sem reprocessar.</li>
 *   <li>{@code refund} em REFUNDED é no-op.</li>
 * </ul>
 */
public class Payment {

    private final String orderId;
    private final Money amount;
    private PaymentStatus status;
    private String reason;

    /** Construtor de reconstituição (usado pelo repositório). */
    public Payment(String orderId, Money amount, PaymentStatus status, String reason) {
        this.orderId = Objects.requireNonNull(orderId, "orderId");
        this.amount = Objects.requireNonNull(amount, "amount");
        this.status = Objects.requireNonNull(status, "status");
        this.reason = reason;
    }

    /**
     * Cria um Payment como AUTHORIZED.
     * Chamado pelo application service após a política de autorização aprovar.
     */
    public static Payment authorize(String orderId, Money amount) {
        return new Payment(orderId, amount, PaymentStatus.AUTHORIZED, null);
    }

    /**
     * Cria um Payment como DECLINED com a razão fornecida.
     * Chamado pelo application service após a política de autorização recusar.
     */
    public static Payment decline(String orderId, Money amount, String reason) {
        Objects.requireNonNull(reason, "reason");
        return new Payment(orderId, amount, PaymentStatus.DECLINED, reason);
    }

    /**
     * Estorna o pagamento: AUTHORIZED → REFUNDED.
     *
     * <p>Idempotente: se já REFUNDED, não faz nada.
     * Lança exceção se o status for DECLINED (não há valor capturado para estornar).
     */
    public void refund() {
        if (status == PaymentStatus.REFUNDED) {
            // Idempotente: já estornado, sem efeito colateral.
            return;
        }
        if (status != PaymentStatus.AUTHORIZED) {
            throw new InvalidPaymentTransitionException(
                    "refund só é possível em AUTHORIZED; status atual: " + status);
        }
        this.status = PaymentStatus.REFUNDED;
    }

    public String getOrderId() {
        return orderId;
    }

    public Money getAmount() {
        return amount;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public String getReason() {
        return reason;
    }

    public boolean isAuthorized() {
        return status == PaymentStatus.AUTHORIZED;
    }

    public boolean isDeclined() {
        return status == PaymentStatus.DECLINED;
    }

    public boolean isRefunded() {
        return status == PaymentStatus.REFUNDED;
    }
}
