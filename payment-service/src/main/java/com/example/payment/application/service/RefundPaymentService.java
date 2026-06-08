package com.example.payment.application.service;

import com.example.payment.application.command.RefundPaymentCommand;
import com.example.payment.domain.exception.PaymentNotFoundException;
import com.example.payment.domain.model.Payment;
import com.example.payment.domain.port.in.RefundPaymentUseCase;
import com.example.payment.domain.port.out.OutboxPort;
import com.example.payment.domain.port.out.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application service: orquestra o estorno de pagamento.
 *
 * <p>Idempotência de negócio: {@link Payment#refund()} é no-op se já REFUNDED,
 * mas o outbox sempre recebe o evento — garantindo que o orquestrador seja notificado
 * mesmo em caso de retry da mensagem.
 */
@Service
class RefundPaymentService implements RefundPaymentUseCase {

    private static final Logger log = LoggerFactory.getLogger(RefundPaymentService.class);

    private final PaymentRepository paymentRepository;
    private final OutboxPort outbox;

    RefundPaymentService(PaymentRepository paymentRepository, OutboxPort outbox) {
        this.paymentRepository = paymentRepository;
        this.outbox = outbox;
    }

    @Override
    @Transactional
    public void execute(RefundPaymentCommand command) {
        Payment payment = paymentRepository.findByOrderId(command.orderId())
                .orElseThrow(() -> new PaymentNotFoundException(command.orderId()));

        payment.refund(); // no-op se já REFUNDED; lança se DECLINED

        paymentRepository.save(payment);
        outbox.registerRefunded(payment.getOrderId());

        log.info("pagamento estornado orderId={}", command.orderId());
    }
}
