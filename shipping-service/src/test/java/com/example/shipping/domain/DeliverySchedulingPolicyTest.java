package com.example.shipping.domain;

import com.example.shipping.domain.model.DeliveryStatus;
import com.example.shipping.domain.service.DeliverySchedulingPolicy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes do domain service DeliverySchedulingPolicy — sem Spring, sem infraestrutura.
 */
class DeliverySchedulingPolicyTest {

    private static final String ORDER_ID = "test-order-001";

    @Test
    @DisplayName("política padrão (simulateFailure=false) aprova o agendamento")
    void decide_defaultPolicy_returnsScheduled() {
        DeliverySchedulingPolicy policy = new DeliverySchedulingPolicy(false);

        DeliverySchedulingPolicy.SchedulingDecision decision = policy.decide(ORDER_ID);

        assertThat(decision.status()).isEqualTo(DeliveryStatus.SCHEDULED);
        assertThat(decision.reason()).isNull();
        assertThat(decision.isSuccess()).isTrue();
    }

    @Test
    @DisplayName("política com simulateFailure=true retorna FAILED com reason")
    void decide_simulateFailure_returnsFailed() {
        DeliverySchedulingPolicy policy = new DeliverySchedulingPolicy(true);

        DeliverySchedulingPolicy.SchedulingDecision decision = policy.decide(ORDER_ID);

        assertThat(decision.status()).isEqualTo(DeliveryStatus.FAILED);
        assertThat(decision.reason()).isNotBlank();
        assertThat(decision.isSuccess()).isFalse();
    }

    @Test
    @DisplayName("reason de falha simulada menciona o orderId")
    void decide_simulateFailure_reasonContainsOrderId() {
        DeliverySchedulingPolicy policy = new DeliverySchedulingPolicy(true);

        DeliverySchedulingPolicy.SchedulingDecision decision = policy.decide(ORDER_ID);

        assertThat(decision.reason()).contains(ORDER_ID);
    }
}
