package com.example.payment.domain.port.in;

import com.example.payment.application.command.RefundPaymentCommand;

/** Porta de entrada: processa o comando de estorno de pagamento. */
public interface RefundPaymentUseCase {
    void execute(RefundPaymentCommand command);
}
