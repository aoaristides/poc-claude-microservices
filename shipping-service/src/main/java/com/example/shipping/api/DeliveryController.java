package com.example.shipping.api;

import com.example.shipping.domain.exception.DeliveryNotFoundException;
import com.example.shipping.domain.model.Delivery;
import com.example.shipping.domain.port.out.DeliveryRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint de debug: consulta o estado de uma entrega por orderId.
 *
 * <p>Não faz parte do contrato da saga — serve exclusivamente para inspeção manual
 * e validação do comportamento durante desenvolvimento.
 */
@RestController
@RequestMapping("/deliveries")
class DeliveryController {

    private final DeliveryRepository deliveryRepository;

    DeliveryController(DeliveryRepository deliveryRepository) {
        this.deliveryRepository = deliveryRepository;
    }

    @GetMapping("/{orderId}")
    ResponseEntity<DeliveryResponse> getDelivery(@PathVariable String orderId) {
        Delivery delivery = deliveryRepository.findByOrderId(orderId)
                .orElseThrow(() -> new DeliveryNotFoundException(orderId));

        return ResponseEntity.ok(new DeliveryResponse(
                delivery.getOrderId(),
                delivery.getStatus(),
                delivery.getTrackingCode(),
                delivery.getReason()));
    }
}
