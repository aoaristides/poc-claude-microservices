package com.example.shipping.domain.model;

/** Estados possíveis de uma entrega. Transição é feita pelo aggregate {@link Delivery}. */
public enum DeliveryStatus {
    SCHEDULED,
    FAILED
}
