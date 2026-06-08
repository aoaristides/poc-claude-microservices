package com.example.payment.api;

import com.example.payment.domain.exception.PaymentNotFoundException;
import com.example.payment.domain.port.out.PaymentRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint de debug: consulta o estado de um pagamento pelo orderId.
 * Não faz parte do fluxo da saga; útil para inspeção manual.
 */
@RestController
@RequestMapping("/payments")
class PaymentController {

    private final PaymentRepository paymentRepository;

    PaymentController(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @GetMapping("/{orderId}")
    ResponseEntity<PaymentResponse> getByOrderId(@PathVariable String orderId) {
        return paymentRepository.findByOrderId(orderId)
                .map(PaymentResponse::from)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new PaymentNotFoundException(orderId));
    }
}
