package com.example.shipping.domain;

import com.example.shipping.domain.exception.DeliveryAlreadyScheduledException;
import com.example.shipping.domain.model.Delivery;
import com.example.shipping.domain.model.DeliveryStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testes de domínio puro — sem Spring, sem infraestrutura.
 * Verificam o comportamento do aggregate Delivery e do gerador de trackingCode.
 */
class DeliveryTest {

    private static final String ORDER_ID = "550e8400-e29b-41d4-a716-446655440000";

    @Test
    @DisplayName("schedule com SCHEDULED define status e trackingCode")
    void schedule_success_setsStatusAndTrackingCode() {
        Delivery delivery = Delivery.newDelivery(ORDER_ID);
        String trackingCode = Delivery.generateTrackingCode(ORDER_ID);

        delivery.schedule(DeliveryStatus.SCHEDULED, trackingCode, null);

        assertThat(delivery.getStatus()).isEqualTo(DeliveryStatus.SCHEDULED);
        assertThat(delivery.getTrackingCode()).isEqualTo(trackingCode);
        assertThat(delivery.getReason()).isNull();
    }

    @Test
    @DisplayName("schedule com FAILED define status e reason, trackingCode permanece nulo")
    void schedule_failure_setsStatusAndReason() {
        Delivery delivery = Delivery.newDelivery(ORDER_ID);

        delivery.schedule(DeliveryStatus.FAILED, null, "CEP fora da área de cobertura");

        assertThat(delivery.getStatus()).isEqualTo(DeliveryStatus.FAILED);
        assertThat(delivery.getTrackingCode()).isNull();
        assertThat(delivery.getReason()).isEqualTo("CEP fora da área de cobertura");
    }

    @Test
    @DisplayName("trackingCode é determinístico para o mesmo orderId")
    void generateTrackingCode_isDeterministic() {
        String code1 = Delivery.generateTrackingCode(ORDER_ID);
        String code2 = Delivery.generateTrackingCode(ORDER_ID);

        assertThat(code1).isEqualTo(code2);
        assertThat(code1).startsWith("TRK-");
    }

    @Test
    @DisplayName("trackingCode usa os 8 primeiros chars do UUID sem hífens em maiúsculas")
    void generateTrackingCode_usesFirst8CharsStripped() {
        // ORDER_ID sem hífens começa com "550e8400..."
        String code = Delivery.generateTrackingCode(ORDER_ID);

        assertThat(code).isEqualTo("TRK-550E8400");
    }

    @Test
    @DisplayName("trackingCodes diferentes para orderIds diferentes")
    void generateTrackingCode_differsByOrderId() {
        String code1 = Delivery.generateTrackingCode("aaaaaaaa-0000-0000-0000-000000000000");
        String code2 = Delivery.generateTrackingCode("bbbbbbbb-0000-0000-0000-000000000000");

        assertThat(code1).isNotEqualTo(code2);
    }

    @Test
    @DisplayName("segundo schedule lança DeliveryAlreadyScheduledException")
    void schedule_twice_throwsException() {
        Delivery delivery = Delivery.newDelivery(ORDER_ID);
        delivery.schedule(DeliveryStatus.SCHEDULED, Delivery.generateTrackingCode(ORDER_ID), null);

        assertThatThrownBy(() ->
                delivery.schedule(DeliveryStatus.SCHEDULED, "TRK-OTHER", null))
                .isInstanceOf(DeliveryAlreadyScheduledException.class)
                .hasMessageContaining(ORDER_ID);
    }

    @Test
    @DisplayName("newDelivery cria delivery sem status")
    void newDelivery_hasNullStatus() {
        Delivery delivery = Delivery.newDelivery(ORDER_ID);

        assertThat(delivery.getStatus()).isNull();
        assertThat(delivery.getOrderId()).isEqualTo(ORDER_ID);
    }
}
