package com.example.inventory.application.service;

import com.example.inventory.application.command.ReserveStockCommand;
import com.example.inventory.domain.exception.InsufficientStockException;
import com.example.inventory.domain.exception.StockItemNotFoundException;
import com.example.inventory.domain.model.Reservation;
import com.example.inventory.domain.model.ReservedItem;
import com.example.inventory.domain.model.StockItem;
import com.example.inventory.domain.port.in.ReserveStockUseCase;
import com.example.inventory.domain.port.out.OutboxPort;
import com.example.inventory.domain.port.out.ReservationRepository;
import com.example.inventory.domain.port.out.StockItemRepository;
import com.example.inventory.domain.service.StockReservationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * Application Service: orquestra a reserva de estoque.
 *
 * <p>Responsabilidades:
 * <ul>
 *   <li>Idempotência de negócio: se já existe RESERVED para o orderId, responde StockReserved
 *       sem decrementar de novo.</li>
 *   <li>Delega a lógica all-or-nothing ao {@link StockReservationService}.</li>
 *   <li>Persiste Reservation + StockItems + Outbox na MESMA transação.</li>
 * </ul>
 */
@Service
public class ReserveStockService implements ReserveStockUseCase {

    private static final Logger log = LoggerFactory.getLogger(ReserveStockService.class);

    private final StockItemRepository stockItemRepository;
    private final ReservationRepository reservationRepository;
    private final OutboxPort outbox;
    private final StockReservationService domainService;

    public ReserveStockService(StockItemRepository stockItemRepository,
                               ReservationRepository reservationRepository,
                               OutboxPort outbox,
                               StockReservationService domainService) {
        this.stockItemRepository = stockItemRepository;
        this.reservationRepository = reservationRepository;
        this.outbox = outbox;
        this.domainService = domainService;
    }

    @Override
    @Transactional
    public void execute(ReserveStockCommand command) {
        String orderId = command.orderId();

        // Idempotência de negócio: reserva já existe e está RESERVED → responde sem decrementar
        var existing = reservationRepository.findByOrderId(orderId);
        if (existing.isPresent() && existing.get().isReserved()) {
            log.info("reserva já existe para orderId={}, respondendo StockReserved (idempotente)", orderId);
            outbox.registerStockReserved(orderId);
            return;
        }

        List<ReservedItem> itemsToReserve = command.items().stream()
                .map(i -> new ReservedItem(i.sku(), i.quantity()))
                .toList();

        List<String> skus = itemsToReserve.stream().map(ReservedItem::sku).toList();
        List<StockItem> loaded = stockItemRepository.findBySkus(skus);
        Map<String, StockItem> stockMap = StockReservationService.indexBySku(loaded);

        try {
            List<ReservedItem> reserved = domainService.reserveAll(itemsToReserve, stockMap);

            // Persiste o estado atualizado dos StockItems
            stockItemRepository.saveAll(loaded);

            // Persiste a reserva para dedup futura e para o release
            var reservation = new Reservation(orderId, reserved);
            reservationRepository.save(reservation);

            // Outbox na mesma transação
            outbox.registerStockReserved(orderId);
            log.info("estoque reservado com sucesso para orderId={}", orderId);

        } catch (StockItemNotFoundException ex) {
            log.warn("SKU não encontrado ao reservar para orderId={}: {}", orderId, ex.getMessage());
            outbox.registerStockUnavailable(orderId, ex.getMessage());

        } catch (InsufficientStockException ex) {
            log.warn("estoque insuficiente ao reservar para orderId={}: sku={} disponível={} solicitado={}",
                    orderId, ex.getSku(), ex.getAvailable(), ex.getRequested());
            outbox.registerStockUnavailable(orderId,
                    "Estoque insuficiente: sku=%s disponível=%d solicitado=%d"
                            .formatted(ex.getSku(), ex.getAvailable(), ex.getRequested()));
        }
    }
}
