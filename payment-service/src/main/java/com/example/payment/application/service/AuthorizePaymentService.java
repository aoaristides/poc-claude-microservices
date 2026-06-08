package com.example.payment.application.service;

import com.example.payment.application.command.AuthorizePaymentCommand;
import com.example.payment.domain.model.AuthorizationResult;
import com.example.payment.domain.model.Money;
import com.example.payment.domain.model.Payment;
import com.example.payment.domain.model.PaymentAuthorizationPolicy;
import com.example.payment.domain.port.in.AuthorizePaymentUseCase;
import com.example.payment.domain.port.out.OutboxPort;
import com.example.payment.domain.port.out.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Application service: orquestra a autorização de pagamento.
 *
 * <p>Idempotência de negócio: se já existe um Payment para o orderId,
 * re-publica o resultado anterior sem reavaliar a política.
 *
 * <p>A transação engloba: consulta, possível persistência do Payment e
 * registro no outbox. O ack Kafka só ocorre depois do commit.
 */
@Service
class AuthorizePaymentService implements AuthorizePaymentUseCase {

    private static final Logger log = LoggerFactory.getLogger(AuthorizePaymentService.class);

    private final PaymentRepository paymentRepository;
    private final OutboxPort outbox;
    private final PaymentAuthorizationPolicy authorizationPolicy;

    AuthorizePaymentService(PaymentRepository paymentRepository,
                            OutboxPort outbox,
                            PaymentAuthorizationPolicy authorizationPolicy) {
        this.paymentRepository = paymentRepository;
        this.outbox = outbox;
        this.authorizationPolicy = authorizationPolicy;
    }

    @Override
    @Transactional
    public void execute(AuthorizePaymentCommand command) {
        Optional<Payment> existing = paymentRepository.findByOrderId(command.orderId());

        if (existing.isPresent()) {
            // Idempotência: re-publica resultado anterior sem reprocessar regra de negócio.
            replyExisting(existing.get());
            return;
        }

        Money money = Money.of(command.amount(), command.currency());
        AuthorizationResult result = authorizationPolicy.evaluate(money);

        Payment payment = switch (result) {
            case AuthorizationResult.Approved ignored -> {
                log.info("pagamento autorizado para orderId={} amount={}", command.orderId(), command.amount());
                yield Payment.authorize(command.orderId(), money);
            }
            case AuthorizationResult.Declined d -> {
                log.info("pagamento recusado para orderId={} reason={}", command.orderId(), d.reason());
                yield Payment.decline(command.orderId(), money, d.reason());
            }
        };

        paymentRepository.save(payment);
        publishReply(payment);
    }

    private void replyExisting(Payment payment) {
        log.debug("replicando resultado existente orderId={} status={}", payment.getOrderId(), payment.getStatus());
        publishReply(payment);
    }

    private void publishReply(Payment payment) {
        switch (payment.getStatus()) {
            case AUTHORIZED -> outbox.registerAuthorized(payment.getOrderId());
            case DECLINED -> outbox.registerDeclined(payment.getOrderId(), payment.getReason());
            case REFUNDED -> outbox.registerRefunded(payment.getOrderId());
        }
    }
}
