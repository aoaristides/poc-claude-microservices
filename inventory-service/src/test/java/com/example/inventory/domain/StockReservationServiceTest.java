package com.example.inventory.domain;

import com.example.inventory.domain.exception.InsufficientStockException;
import com.example.inventory.domain.exception.StockItemNotFoundException;
import com.example.inventory.domain.model.Reservation;
import com.example.inventory.domain.model.ReservationStatus;
import com.example.inventory.domain.model.ReservedItem;
import com.example.inventory.domain.model.StockItem;
import com.example.inventory.domain.service.StockReservationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testes de unidade do StockReservationService (domain service).
 * Foco: garantia do comportamento all-or-nothing.
 * Sem Spring — domínio puro.
 */
class StockReservationServiceTest {

    private StockReservationService service;

    @BeforeEach
    void setUp() {
        service = new StockReservationService();
    }

    @Test
    @DisplayName("reserveAll deve decrementar todos os itens quando há estoque suficiente")
    void reserveAll_sufficientStock_decrementsAll() {
        var sku1 = new StockItem("SKU-1", 100);
        var sku2 = new StockItem("SKU-2", 50);
        Map<String, StockItem> stockMap = Map.of("SKU-1", sku1, "SKU-2", sku2);

        var items = List.of(new ReservedItem("SKU-1", 10), new ReservedItem("SKU-2", 20));

        var result = service.reserveAll(items, stockMap);

        assertThat(sku1.getAvailableQuantity()).isEqualTo(90);
        assertThat(sku2.getAvailableQuantity()).isEqualTo(30);
        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("reserveAll não deve decrementar NADA quando um item tem estoque insuficiente (all-or-nothing)")
    void reserveAll_oneItemInsufficient_decrementsNothing() {
        var sku1 = new StockItem("SKU-1", 100);
        var sku2 = new StockItem("SKU-2", 5);     // insuficiente para qty=10
        Map<String, StockItem> stockMap = Map.of("SKU-1", sku1, "SKU-2", sku2);

        var items = List.of(new ReservedItem("SKU-1", 10), new ReservedItem("SKU-2", 10));

        assertThatThrownBy(() -> service.reserveAll(items, stockMap))
                .isInstanceOf(InsufficientStockException.class);

        // CRÍTICO: SKU-1 não pode ter sido decrementado (all-or-nothing)
        assertThat(sku1.getAvailableQuantity()).isEqualTo(100);
        assertThat(sku2.getAvailableQuantity()).isEqualTo(5);
    }

    @Test
    @DisplayName("reserveAll deve lançar StockItemNotFoundException quando SKU não existe no mapa")
    void reserveAll_unknownSku_throwsNotFoundException() {
        Map<String, StockItem> stockMap = Map.of("SKU-1", new StockItem("SKU-1", 100));

        var items = List.of(new ReservedItem("SKU-INEXISTENTE", 5));

        assertThatThrownBy(() -> service.reserveAll(items, stockMap))
                .isInstanceOf(StockItemNotFoundException.class)
                .hasMessageContaining("SKU-INEXISTENTE");
    }

    @Test
    @DisplayName("releaseAll deve incrementar estoque de todos os itens e marcar reserva como RELEASED")
    void releaseAll_incrementsStockAndReleasesReservation() {
        var sku1 = new StockItem("SKU-1", 90);
        var sku2 = new StockItem("SKU-2", 30);
        Map<String, StockItem> stockMap = Map.of("SKU-1", sku1, "SKU-2", sku2);

        var reservedItems = List.of(new ReservedItem("SKU-1", 10), new ReservedItem("SKU-2", 20));
        var reservation = new Reservation("order-1", ReservationStatus.RESERVED,
                reservedItems, Instant.now());

        service.releaseAll(reservation, stockMap);

        assertThat(sku1.getAvailableQuantity()).isEqualTo(100);
        assertThat(sku2.getAvailableQuantity()).isEqualTo(50);
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.RELEASED);
    }

    @Test
    @DisplayName("indexBySku deve criar mapa SKU → StockItem corretamente")
    void indexBySku_createsCorrectMap() {
        var items = List.of(new StockItem("SKU-1", 100), new StockItem("SKU-2", 50));

        var map = StockReservationService.indexBySku(items);

        assertThat(map).containsKey("SKU-1");
        assertThat(map).containsKey("SKU-2");
        assertThat(map.get("SKU-1").getAvailableQuantity()).isEqualTo(100);
    }
}
