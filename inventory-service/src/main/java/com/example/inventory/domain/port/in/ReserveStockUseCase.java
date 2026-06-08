package com.example.inventory.domain.port.in;

import com.example.inventory.application.command.ReserveStockCommand;

/** Caso de uso de entrada: reserva de estoque para um pedido. */
public interface ReserveStockUseCase {

    void execute(ReserveStockCommand command);
}
