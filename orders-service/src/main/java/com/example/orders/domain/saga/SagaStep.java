package com.example.orders.domain.saga;

/** Passos que, uma vez concluídos, precisam ser compensados em caso de falha. */
public enum SagaStep {
    STOCK_RESERVED,
    PAYMENT_AUTHORIZED,
    DELIVERY_SCHEDULED
}
