package com.example.inventory.domain.service;

import com.example.inventory.domain.exception.InsufficientStockException;
import com.example.inventory.domain.exception.StockItemNotFoundException;
import com.example.inventory.domain.model.Reservation;
import com.example.inventory.domain.model.ReservedItem;
import com.example.inventory.domain.model.StockItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Domain Service: lógica de reserva all-or-nothing.
 *
 * <p>Regras:
 * <ul>
 *   <li>Primeiro verifica disponibilidade de TODOS os itens sem alterar estado.</li>
 *   <li>Só decrementa se todos couberem — sem reserva parcial.</li>
 *   <li>Se algum item faltar, não altera nada e sinaliza qual SKU causou a falha.</li>
 * </ul>
 *
 * <p>Este service não tem acesso a repositórios — recebe os objetos prontos como
 * parâmetro, respeitando o princípio de que a orquestração de IO fica na camada
 * de aplicação.
 */
public class StockReservationService {

    /**
     * Executa a reserva all-or-nothing.
     *
     * @param itemsToReserve lista de (sku, qty) solicitados pelo orquestrador
     * @param stockItems     mapa de SKU → StockItem carregados do repositório
     * @return lista de ReservedItem representando o que foi decrementado
     * @throws StockItemNotFoundException se algum SKU solicitado não existir no mapa
     * @throws InsufficientStockException se algum SKU não tiver estoque suficiente
     */
    public List<ReservedItem> reserveAll(
            List<ReservedItem> itemsToReserve,
            Map<String, StockItem> stockItems) {

        // Fase 1: verificação de disponibilidade (read-only, não altera estado)
        for (ReservedItem item : itemsToReserve) {
            StockItem stock = stockItems.get(item.sku());
            if (stock == null) {
                throw new StockItemNotFoundException(item.sku());
            }
            if (stock.getAvailableQuantity() < item.quantity()) {
                throw new InsufficientStockException(
                        item.sku(), stock.getAvailableQuantity(), item.quantity());
            }
        }

        // Fase 2: decremento atômico — só chega aqui se TODOS passaram na fase 1
        for (ReservedItem item : itemsToReserve) {
            stockItems.get(item.sku()).reserve(item.quantity());
        }

        return List.copyOf(itemsToReserve);
    }

    /**
     * Estorna as quantidades de uma reserva nos StockItems correspondentes.
     *
     * @param reservation reserva a ser liberada
     * @param stockItems  mapa de SKU → StockItem carregados do repositório
     */
    public void releaseAll(Reservation reservation, Map<String, StockItem> stockItems) {
        for (ReservedItem item : reservation.getItems()) {
            StockItem stock = stockItems.get(item.sku());
            if (stock != null) {
                stock.release(item.quantity());
            }
            // SKU removido do catálogo após a reserva: ignora silenciosamente
            // (não podemos reverter o que não existe mais)
        }
        reservation.release();
    }

    /** Converte lista para mapa SKU → StockItem (utilitário para o application service). */
    public static Map<String, StockItem> indexBySku(List<StockItem> items) {
        return items.stream().collect(Collectors.toMap(StockItem::getSku, Function.identity()));
    }
}
