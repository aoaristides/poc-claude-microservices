package com.example.shipping.application;

import com.example.shipping.application.service.ScheduleDeliveryService;
import com.example.shipping.domain.model.Delivery;
import com.example.shipping.domain.model.DeliveryStatus;
import com.example.shipping.domain.port.out.DeliveryRepository;
import com.example.shipping.domain.port.out.OutboxPort;
import com.example.shipping.domain.service.DeliverySchedulingPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testes da camada de aplicação com mocks das portas.
 * Sem Spring: verificam orquestração, idempotência e integração com a política de domínio.
 */
@ExtendWith(MockitoExtension.class)
class ScheduleDeliveryServiceTest {

    private static final String ORDER_ID = "550e8400-e29b-41d4-a716-446655440000";

    @Mock
    private DeliveryRepository deliveryRepository;

    @Mock
    private OutboxPort outboxPort;

    private ScheduleDeliveryService serviceSuccess;
    private ScheduleDeliveryService serviceFailure;

    @BeforeEach
    void setUp() {
        serviceSuccess = new ScheduleDeliveryService(
                deliveryRepository, outboxPort,
                new DeliverySchedulingPolicy(false, "SKU-FAIL-SHIP"));
        serviceFailure = new ScheduleDeliveryService(
                deliveryRepository, outboxPort,
                new DeliverySchedulingPolicy(true, "SKU-FAIL-SHIP"));
    }

    @Test
    @DisplayName("ScheduleDelivery (sucesso) → persiste delivery SCHEDULED e registra DeliveryScheduled no outbox")
    void execute_success_savesDeliveryAndRegistersOutbox() {
        when(deliveryRepository.findByOrderId(ORDER_ID)).thenReturn(Optional.empty());

        serviceSuccess.execute(ORDER_ID, List.of());

        // Verifica que o delivery salvo está com status SCHEDULED e tem trackingCode
        ArgumentCaptor<Delivery> captor = ArgumentCaptor.forClass(Delivery.class);
        verify(deliveryRepository).save(captor.capture());
        Delivery saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(DeliveryStatus.SCHEDULED);
        assertThat(saved.getTrackingCode()).isEqualTo(Delivery.generateTrackingCode(ORDER_ID));
        assertThat(saved.getReason()).isNull();

        // Verifica que o outbox foi registrado com o mesmo aggregate
        verify(outboxPort).registerReply(argThat(d ->
                d.getOrderId().equals(ORDER_ID) &&
                d.getStatus() == DeliveryStatus.SCHEDULED));
    }

    @Test
    @DisplayName("ScheduleDelivery com simulate-failure → persiste FAILED e registra DeliveryFailed no outbox")
    void execute_simulateFailure_savesFailedAndRegistersOutbox() {
        when(deliveryRepository.findByOrderId(ORDER_ID)).thenReturn(Optional.empty());

        serviceFailure.execute(ORDER_ID, List.of());

        ArgumentCaptor<Delivery> captor = ArgumentCaptor.forClass(Delivery.class);
        verify(deliveryRepository).save(captor.capture());
        Delivery saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(DeliveryStatus.FAILED);
        assertThat(saved.getTrackingCode()).isNull();
        assertThat(saved.getReason()).isNotBlank();

        verify(outboxPort).registerReply(argThat(d ->
                d.getOrderId().equals(ORDER_ID) &&
                d.getStatus() == DeliveryStatus.FAILED));
    }

    @Test
    @DisplayName("ScheduleDelivery idempotente: segundo ScheduleDelivery re-publica resultado sem reprocessar")
    void execute_idempotent_republishesExistingResultWithoutReprocessing() {
        // Simula delivery já salvo como SCHEDULED
        Delivery existing = Delivery.newDelivery(ORDER_ID);
        existing.schedule(DeliveryStatus.SCHEDULED, Delivery.generateTrackingCode(ORDER_ID), null);
        when(deliveryRepository.findByOrderId(ORDER_ID)).thenReturn(Optional.of(existing));

        // Segunda chamada (política de falha, mas não importa — já existe)
        serviceFailure.execute(ORDER_ID, List.of());

        // NÃO deve salvar novamente
        verify(deliveryRepository, never()).save(any());

        // Deve re-publicar o resultado existente (SCHEDULED, não FAILED)
        verify(outboxPort).registerReply(argThat(d ->
                d.getOrderId().equals(ORDER_ID) &&
                d.getStatus() == DeliveryStatus.SCHEDULED));
    }

    @Test
    @DisplayName("ScheduleDelivery idempotente: re-publica resultado FAILED sem reprocessar")
    void execute_idempotent_republishesExistingFailedResult() {
        Delivery existing = Delivery.newDelivery(ORDER_ID);
        existing.schedule(DeliveryStatus.FAILED, null, "motivo original");
        when(deliveryRepository.findByOrderId(ORDER_ID)).thenReturn(Optional.of(existing));

        serviceSuccess.execute(ORDER_ID, List.of());  // política de sucesso, mas já existe FAILED

        verify(deliveryRepository, never()).save(any());
        verify(outboxPort).registerReply(argThat(d ->
                d.getOrderId().equals(ORDER_ID) &&
                d.getStatus() == DeliveryStatus.FAILED &&
                "motivo original".equals(d.getReason())));
    }
}
