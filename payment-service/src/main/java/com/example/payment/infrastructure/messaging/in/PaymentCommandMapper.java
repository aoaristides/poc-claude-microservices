package com.example.payment.infrastructure.messaging.in;

import com.example.payment.application.command.AuthorizePaymentCommand;
import com.example.payment.application.command.RefundPaymentCommand;
import com.example.payment.infrastructure.messaging.InvalidPaymentEventException;
import com.example.payment.infrastructure.messaging.contract.AuthorizePaymentMessage;
import com.example.payment.infrastructure.messaging.contract.RefundPaymentMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * Anti-Corruption Layer de entrada: traduz a mensagem do broker
 * (header eventType + payload JSON) para comandos de domínio.
 *
 * <p>O domínio não conhece os nomes externos dos comandos dos participantes.
 */
@Component
class PaymentCommandMapper {

    private final ObjectMapper mapper;

    PaymentCommandMapper(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * Classifica e converte o record em um dos comandos de domínio suportados.
     *
     * @return {@link AuthorizePaymentCommand} ou {@link RefundPaymentCommand}
     */
    Object toDomainCommand(ConsumerRecord<String, String> record) {
        String eventType = header(record, "eventType");
        return switch (eventType) {
            case "AuthorizePayment" -> parseAuthorize(record.value());
            case "RefundPayment" -> parseRefund(record.value());
            default -> throw new InvalidPaymentEventException("eventType desconhecido: " + eventType);
        };
    }

    private AuthorizePaymentCommand parseAuthorize(String value) {
        AuthorizePaymentMessage msg = parse(value, AuthorizePaymentMessage.class);
        return new AuthorizePaymentCommand(msg.orderId(), msg.amount(), msg.currency());
    }

    private RefundPaymentCommand parseRefund(String value) {
        RefundPaymentMessage msg = parse(value, RefundPaymentMessage.class);
        return new RefundPaymentCommand(msg.orderId());
    }

    private <T> T parse(String value, Class<T> type) {
        try {
            return mapper.readValue(value, type);
        } catch (Exception e) {
            throw new InvalidPaymentEventException("payload inválido para " + type.getSimpleName(), e);
        }
    }

    private String header(ConsumerRecord<?, ?> record, String name) {
        var h = record.headers().lastHeader(name);
        if (h == null) {
            throw new InvalidPaymentEventException("header ausente: " + name);
        }
        return new String(h.value(), StandardCharsets.UTF_8);
    }
}
