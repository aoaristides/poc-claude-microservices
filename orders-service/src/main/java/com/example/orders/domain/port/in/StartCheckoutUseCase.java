package com.example.orders.domain.port.in;

import com.example.orders.application.command.StartCheckoutCommand;
import com.example.orders.domain.model.OrderId;

/** Porta de entrada: iniciar o checkout (confirma pedido + inicia a saga). */
public interface StartCheckoutUseCase {

    OrderId execute(StartCheckoutCommand command);
}
