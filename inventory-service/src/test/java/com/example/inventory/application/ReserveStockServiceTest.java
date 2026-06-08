package com.example.inventory.application;

import com.example.inventory.application.command.ReserveStockCommand;
import com.example.inventory.application.service.ReserveStockService;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testes do ReserveStockService com mocks das portas de saída.
 * Verifica o fluxo de aplicação sem contexto Spring.
 */
@ExtendWith(MockitoExtension.class)
class ReserveStockServiceTest {

    @Mock
    private StockItemRepository stockItemRepository;

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private OutboxPort outbox;

    private ReserveStockService service;

    @BeforeEach
    void setUp() {
        service = new ReserveStockService(
                stockItemRepository,
                reservationRepository,
                outbox,
                new StockReservationService());
    }

    @Test
    @DisplayName("execute com estoque suficiente deve persistir reserva e registrar StockReserved no outbox")
    void execute_sufficientStock_registersStockReserved() {
        String orderId = "order-abc";
        var command = new ReserveStockCommand(orderId,
                List.of(new ReserveStockCommand.Item("SKU-1", 5)));

        when(reservationRepository.findByOrderId(orderId)).thenReturn(Optional.empty());
        when(stockItemRepository.findBySkus(List.of("SKU-1")))
                .thenReturn(List.of(new StockItem("SKU-1", 100)));

        service.execute(command);

        // Verifica que uma Reservation foi salva com status RESERVED
        var captor = ArgumentCaptor.forClass(Reservation.class);
        verify(reservationRepository).save(captor.capture());
        assertThat(captor.getValue().getOrderId()).isEqualTo(orderId);
        assertThat(captor.getValue().getStatus()).isEqualTo(ReservationStatus.RESERVED);

        // Verifica que o outbox recebeu StockReserved
        verify(outbox).registerStockReserved(orderId);
        verify(outbox, never()).registerStockUnavailable(anyString(), anyString());
    }

    @Test
    @DisplayName("execute com estoque insuficiente deve registrar StockUnavailable no outbox")
    void execute_insufficientStock_registersStockUnavailable() {
        String orderId = "order-xyz";
        var command = new ReserveStockCommand(orderId,
                List.of(new ReserveStockCommand.Item("SKU-1", 999)));

        when(reservationRepository.findByOrderId(orderId)).thenReturn(Optional.empty());
        when(stockItemRepository.findBySkus(List.of("SKU-1")))
                .thenReturn(List.of(new StockItem("SKU-1", 10)));

        service.execute(command);

        verify(outbox).registerStockUnavailable(anyString(), anyString());
        verify(outbox, never()).registerStockReserved(anyString());
        verify(reservationRepository, never()).save(any());
    }

    @Test
    @DisplayName("execute com SKU inexistente deve registrar StockUnavailable no outbox")
    void execute_unknownSku_registersStockUnavailable() {
        String orderId = "order-unknown-sku";
        var command = new ReserveStockCommand(orderId,
                List.of(new ReserveStockCommand.Item("SKU-NAOEXISTE", 1)));

        when(reservationRepository.findByOrderId(orderId)).thenReturn(Optional.empty());
        // Retorna lista vazia: SKU não encontrado no banco
        when(stockItemRepository.findBySkus(anyList())).thenReturn(List.of());

        service.execute(command);

        verify(outbox).registerStockUnavailable(anyString(), anyString());
        verify(outbox, never()).registerStockReserved(anyString());
    }

    @Test
    @DisplayName("execute idempotente: reserva RESERVED já existente deve responder StockReserved sem decrementar")
    void execute_reservationAlreadyReserved_respondsIdempotently() {
        String orderId = "order-duplicate";
        var command = new ReserveStockCommand(orderId,
                List.of(new ReserveStockCommand.Item("SKU-1", 5)));

        var existingReservation = new Reservation(orderId,
                ReservationStatus.RESERVED,
                List.of(new ReservedItem("SKU-1", 5)),
                Instant.now());
        when(reservationRepository.findByOrderId(orderId))
                .thenReturn(Optional.of(existingReservation));

        service.execute(command);

        // Não deve consultar nem alterar o estoque
        verify(stockItemRepository, never()).findBySkus(anyList());
        verify(stockItemRepository, never()).saveAll(anyList());
        verify(reservationRepository, never()).save(any());

        // Deve responder StockReserved normalmente (idempotente)
        verify(outbox).registerStockReserved(orderId);
    }
}
