package com.example.inventory.domain.port.out;

import com.example.inventory.domain.model.Reservation;

import java.util.Optional;

/** Porta de saída: persistência de Reservation. */
public interface ReservationRepository {

    Optional<Reservation> findByOrderId(String orderId);

    void save(Reservation reservation);
}
