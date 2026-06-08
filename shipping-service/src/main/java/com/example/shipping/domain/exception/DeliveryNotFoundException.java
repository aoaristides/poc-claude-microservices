package com.example.shipping.domain.exception;

/** Lançada quando não existe entrega para o orderId informado. */
public class DeliveryNotFoundException extends RuntimeException {

    public DeliveryNotFoundException(String orderId) {
        super("entrega não encontrada para orderId=" + orderId);
    }
}
