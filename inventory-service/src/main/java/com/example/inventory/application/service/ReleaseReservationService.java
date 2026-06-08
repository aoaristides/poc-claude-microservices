package com.example.inventory.application.service;

import com.example.inventory.application.command.ReleaseReservationCommand;
import com.example.inventory.domain.model.Reservation;
import com.example.inventory.domain.model.StockItem;
import com.example.inventory.domain.port.in.ReleaseReservationUseCase;
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
 * Application Service: orquestra a liberação de reserva (compensação da saga).
 *
 * <p>Idempotência de negócio:
 * <ul>
 *   <li>Reserva inexistente → no-op, responde ReservationReleased.</li>
 *   <li>Reserva já RELEASED → no-op, responde ReservationReleased.</li>
 * </ul>
 */
@Service
public class ReleaseReservationService implements ReleaseReservationUseCase {

    private static final Logger log = LoggerFactory.getLogger(ReleaseReservationService.class);

    private final ReservationRepository reservationRepository;
    private final StockItemRepository stockItemRepository;
    private final OutboxPort outbox;
    private final StockReservationService domainService;

    public ReleaseReservationService(ReservationRepository reservationRepository,
                                     StockItemRepository stockItemRepository,
                                     OutboxPort outbox,
                                     StockReservationService domainService) {
        this.reservationRepository = reservationRepository;
        this.stockItemRepository = stockItemRepository;
        this.outbox = outbox;
        this.domainService = domainService;
    }

    @Override
    @Transactional
    public void execute(ReleaseReservationCommand command) {
        String orderId = command.orderId();

        var optional = reservationRepository.findByOrderId(orderId);

        // Idempotência: reserva inexistente ou já liberada → responde sem alterar estoque
        if (optional.isEmpty() || !optional.get().isReserved()) {
            log.info("reserva ausente ou já liberada para orderId={}, respondendo ReservationReleased (idempotente)",
                    orderId);
            outbox.registerReservationReleased(orderId);
            return;
        }

        Reservation reservation = optional.get();

        List<String> skus = reservation.getItems().stream()
                .map(item -> item.sku())
                .toList();
        List<StockItem> loaded = stockItemRepository.findBySkus(skus);
        Map<String, StockItem> stockMap = StockReservationService.indexBySku(loaded);

        // Estorna estoque e marca reserva como RELEASED
        domainService.releaseAll(reservation, stockMap);

        stockItemRepository.saveAll(loaded);
        reservationRepository.save(reservation);

        outbox.registerReservationReleased(orderId);
        log.info("reserva liberada com sucesso para orderId={}", orderId);
    }
}
