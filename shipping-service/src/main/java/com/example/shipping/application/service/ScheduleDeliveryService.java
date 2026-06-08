package com.example.shipping.application.service;

import com.example.shipping.domain.model.Delivery;
import com.example.shipping.domain.model.DeliveryStatus;
import com.example.shipping.domain.port.in.ScheduleDeliveryUseCase;
import com.example.shipping.domain.port.out.DeliveryRepository;
import com.example.shipping.domain.port.out.OutboxPort;
import com.example.shipping.domain.service.DeliverySchedulingPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Application service: orquestra o agendamento de entrega.
 *
 * <p>Idempotência de negócio: se o orderId já tem uma entrega salva, re-publica
 * o resultado anterior (outbox) sem reprocessar a política de agendamento.
 * Tudo (delivery + outbox) persiste na MESMA transação.
 */
public class ScheduleDeliveryService implements ScheduleDeliveryUseCase {

    private static final Logger log = LoggerFactory.getLogger(ScheduleDeliveryService.class);

    private final DeliveryRepository deliveryRepository;
    private final OutboxPort outboxPort;
    private final DeliverySchedulingPolicy schedulingPolicy;

    public ScheduleDeliveryService(DeliveryRepository deliveryRepository,
                                   OutboxPort outboxPort,
                                   DeliverySchedulingPolicy schedulingPolicy) {
        this.deliveryRepository = deliveryRepository;
        this.outboxPort = outboxPort;
        this.schedulingPolicy = schedulingPolicy;
    }

    @Override
    @Transactional
    public void execute(String orderId, List<String> skus) {
        Optional<Delivery> existing = deliveryRepository.findByOrderId(orderId);

        if (existing.isPresent()) {
            // Idempotência: re-publica o mesmo resultado sem reprocessar
            Delivery delivery = existing.get();
            log.info("ScheduleDelivery idempotente: orderId={} status={} — re-publicando resultado",
                    orderId, delivery.getStatus());
            outboxPort.registerReply(delivery);
            return;
        }

        DeliverySchedulingPolicy.SchedulingDecision decision = schedulingPolicy.decide(orderId, skus);
        Delivery delivery = Delivery.newDelivery(orderId);

        String trackingCode = decision.isSuccess()
                ? Delivery.generateTrackingCode(orderId)
                : null;

        delivery.schedule(decision.status(), trackingCode, decision.reason());

        deliveryRepository.save(delivery);
        outboxPort.registerReply(delivery);

        log.info("ScheduleDelivery processado: orderId={} status={} trackingCode={}",
                orderId, delivery.getStatus(), delivery.getTrackingCode());
    }
}
