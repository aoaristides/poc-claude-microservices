package com.example.payment.domain.port.in;

import com.example.payment.application.command.AuthorizePaymentCommand;

/** Porta de entrada: processa o comando de autorização de pagamento. */
public interface AuthorizePaymentUseCase {
    void execute(AuthorizePaymentCommand command);
}
