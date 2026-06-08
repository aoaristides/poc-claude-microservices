package com.example.inventory.domain.port.in;

import com.example.inventory.application.command.ReleaseReservationCommand;

/** Caso de uso de entrada: liberação de reserva (compensação da saga). */
public interface ReleaseReservationUseCase {

    void execute(ReleaseReservationCommand command);
}
