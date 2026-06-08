package com.example.shipping.domain.service;

import com.example.shipping.domain.model.DeliveryStatus;

import java.util.List;

/**
 * Domain Service: política de decisão de agendamento.
 *
 * <p>Por padrão, a política aprova a entrega (caminho feliz da saga). Falha quando:
 * <ul>
 *   <li>{@code simulateFailure=true} — força a falha de forma global (útil em DEV); ou</li>
 *   <li>o pedido contém o {@code undeliverableSku} — gatilho determinístico e
 *       <em>per-request</em> para exercitar a compensação sem afetar outros pedidos.</li>
 * </ul>
 *
 * <p>Em um cenário real, esta classe conteria regras como: verificação de CEP atendido,
 * janela de entrega disponível, carrier com capacidade, SKU atendido pela transportadora, etc.
 */
public class DeliverySchedulingPolicy {

    private final boolean simulateFailure;
    private final String undeliverableSku;

    public DeliverySchedulingPolicy(boolean simulateFailure, String undeliverableSku) {
        this.simulateFailure = simulateFailure;
        this.undeliverableSku = undeliverableSku;
    }

    /**
     * Decide o resultado do agendamento a partir do pedido e seus SKUs.
     *
     * @return {@link SchedulingDecision} com status e reason (null quando SCHEDULED)
     */
    public SchedulingDecision decide(String orderId, List<String> skus) {
        if (simulateFailure) {
            return SchedulingDecision.failed("falha simulada de agendamento para orderId=" + orderId);
        }
        if (containsUndeliverable(skus)) {
            return SchedulingDecision.failed(
                    "SKU não atendido pela transportadora: " + undeliverableSku + " (orderId=" + orderId + ")");
        }
        return SchedulingDecision.scheduled();
    }

    private boolean containsUndeliverable(List<String> skus) {
        return undeliverableSku != null && !undeliverableSku.isBlank()
                && skus != null && skus.contains(undeliverableSku);
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
