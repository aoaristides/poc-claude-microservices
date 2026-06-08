package com.example.inventory.domain;

import com.example.inventory.domain.exception.InsufficientStockException;
import com.example.inventory.domain.model.StockItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testes de unidade do aggregate StockItem.
 * Sem Spring — domínio puro.
 */
class StockItemTest {

    @Test
    @DisplayName("reserve deve decrementar a quantidade disponível")
    void reserve_decrementsAvailableQuantity() {
        var item = new StockItem("SKU-1", 100);

        item.reserve(30);

        assertThat(item.getAvailableQuantity()).isEqualTo(70);
    }

    @Test
    @DisplayName("reserve deve lançar InsufficientStockException quando quantidade solicitada excede o disponível")
    void reserve_throwsWhenInsufficientStock() {
        var item = new StockItem("SKU-1", 10);

        assertThatThrownBy(() -> item.reserve(11))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("SKU-1")
                .hasMessageContaining("disponível=10")
                .hasMessageContaining("solicitado=11");
    }

    @Test
    @DisplayName("reserve com quantidade exata disponível deve funcionar (boundary)")
    void reserve_exactAvailableQuantity_succeeds() {
        var item = new StockItem("SKU-2", 50);

        item.reserve(50);

        assertThat(item.getAvailableQuantity()).isZero();
    }

    @Test
    @DisplayName("release deve incrementar a quantidade disponível")
    void release_incrementsAvailableQuantity() {
        var item = new StockItem("SKU-1", 70);

        item.release(30);

        assertThat(item.getAvailableQuantity()).isEqualTo(100);
    }

    @Test
    @DisplayName("reserve seguido de release deve restaurar a quantidade original")
    void reserveThenRelease_restoresQuantity() {
        var item = new StockItem("SKU-3", 200);

        item.reserve(80);
        item.release(80);

        assertThat(item.getAvailableQuantity()).isEqualTo(200);
    }

    @Test
    @DisplayName("reserve com quantidade zero deve lançar IllegalArgumentException")
    void reserve_zeroQuantity_throwsIllegalArgument() {
        var item = new StockItem("SKU-1", 100);

        assertThatThrownBy(() -> item.reserve(0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("InsufficientStockException deve expor sku, available e requested")
    void insufficientStockException_exposesDetails() {
        var item = new StockItem("SKU-4", 5);

        var ex = org.junit.jupiter.api.Assertions.assertThrows(
                InsufficientStockException.class, () -> item.reserve(20));

        assertThat(ex.getSku()).isEqualTo("SKU-4");
        assertThat(ex.getAvailable()).isEqualTo(5);
        assertThat(ex.getRequested()).isEqualTo(20);
    }
}
