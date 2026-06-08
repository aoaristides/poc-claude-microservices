package com.example.shipping.domain.service;

import com.example.shipping.domain.model.DeliveryStatus;

/**
 * Domain Service: política de decisão de agendamento.
 *
 * <p>Por padrão, a política aprova a entrega (caminho feliz da saga).
 * Com {@code simulateFailure=true}, rejeita — útil para exercitar a compensação
 * (estorno de pagamento + liberação de reserva de estoque) sem infraestrutura externa.
 *
 * <p>Em um cenário real, esta classe conteria regras como: verificação de CEP atendido,
 * janela de entrega disponível, carrier com capacidade, etc.
 */
public class DeliverySchedulingPolicy {

    private final boolean simulateFailure;

    public DeliverySchedulingPolicy(boolean simulateFailure) {
        this.simulateFailure = simulateFailure;
    }

    /**
     * Decide o resultado do agendamento.
     *
     * @return {@link SchedulingDecision} com status e reason (null quando SCHEDULED)
     */
    public SchedulingDecision decide(String orderId) {
        if (simulateFailure) {
            return SchedulingDecision.failed("falha simulada de agendamento para orderId=" + orderId);
        }
        return SchedulingDecision.scheduled();
    }

    /** Resultado da decisão de agendamento. */
    public record SchedulingDecision(DeliveryStatus status, String reason) {

        public static SchedulingDecision scheduled() {
            return new SchedulingDecision(DeliveryStatus.SCHEDULED, null);
        }

        public static SchedulingDecision failed(String reason) {
            return new SchedulingDecision(DeliveryStatus.FAILED, reason);
        }

        public boolean isSuccess() {
            return status == DeliveryStatus.SCHEDULED;
        }
    }
}
