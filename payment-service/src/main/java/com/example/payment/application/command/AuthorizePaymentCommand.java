package com.example.payment.application.command;

import java.math.BigDecimal;

/**
 * Comando de autorização de pagamento.
 * Traduzido do contrato Kafka pelo adapter de entrada (ACL).
 */
public record AuthorizePaymentCommand(
        String orderId,
        BigDecimal amount,
        String currency
) {
}
