package com.example.inventory.application;

import com.example.inventory.application.command.ReleaseReservationCommand;
import com.example.inventory.application.service.ReleaseReservationService;
import com.example.inventory.domain.model.Reservation;
import com.example.inventory.domain.model.ReservationStatus;
import com.example.inventory.domain.model.ReservedItem;
import com.example.inventory.domain.model.StockItem;
import com.example.inventory.domain.port.out.OutboxPort;
import com.example.inventory.domain.port.out.ReservationRepository;
import com.example.inventory.domain.port.out.StockItemRepository;
import com.example.inventory.domain.service.StockReservationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testes do ReleaseReservationService com mocks das portas de saída.
 */
@ExtendWith(MockitoExtension.class)
class ReleaseReservationServiceTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private StockItemRepository stockItemRepository;

    @Mock
    private OutboxPort outbox;

    private ReleaseReservationService service;

    @BeforeEach
    void setUp() {
        service = new ReleaseReservationService(
                reservationRepository,
                stockItemRepository,
                outbox,
                new StockReservationService());
    }

    @Test
    @DisplayName("execute com reserva RESERVED deve estornar estoque e registrar ReservationReleased")
    void execute_reservedReservation_releasesStockAndRegistersReply() {
        String orderId = "order-release";
        var command = new ReleaseReservationCommand(orderId);

        var reservation = new Reservation(orderId,
                ReservationStatus.RESERVED,
                List.of(new ReservedItem("SKU-1", 10)),
                Instant.now());

        when(reservationRepository.findByOrderId(orderId)).thenReturn(Optional.of(reservation));
        when(stockItemRepository.findBySkus(List.of("SKU-1")))
                .thenReturn(List.of(new StockItem("SKU-1", 90)));

        service.execute(command);

        verify(stockItemRepository).saveAll(anyList());
        verify(reservationRepository).save(reservation);
        verify(outbox).registerReservationReleased(orderId);
    }

    @Test
    @DisplayName("execute idempotente: reserva RELEASED já existente deve responder ReservationReleased sem alterar estoque")
    void execute_reservationAlreadyReleased_respondsIdempotently() {
        String orderId = "order-already-released";
        var command = new ReleaseReservationCommand(orderId);

        var released = new Reservation(orderId,
                ReservationStatus.RELEASED,
                List.of(new ReservedItem("SKU-1", 10)),
                Instant.now());

        when(reservationRepository.findByOrderId(orderId)).thenReturn(Optional.of(released));

        service.execute(command);

        // Não deve tocar no estoque
        verify(stockItemRepository, never()).findBySkus(anyList());
        verify(stockItemRepository, never()).saveAll(anyList());
        verify(reservationRepository, never()).save(any());

        // Deve responder normalmente (idempotente)
        verify(outbox).registerReservationReleased(orderId);
    }

    @Test
    @DisplayName("execute idempotente: reserva inexistente deve responder ReservationReleased (no-op)")
    void execute_noReservationFound_respondsIdempotently() {
        String orderId = "order-nonexistent";
        var command = new ReleaseReservationCommand(orderId);

        when(reservationRepository.findByOrderId(orderId)).thenReturn(Optional.empty());

        service.execute(command);

        verify(stockItemRepository, never()).findBySkus(anyList());
        verify(stockItemRepository, never()).saveAll(anyList());
        verify(outbox).registerReservationReleased(orderId);
    }
}
