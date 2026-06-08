package com.example.orders.domain.model;

/** Ciclo de vida do pedido no contexto de Orders. */
public enum OrderStatus {
    DRAFT,       // rascunho (não usado no fluxo de checkout direto, mantido para extensão)
    CONFIRMED,   // confirmado: saga de checkout em andamento
    PAID,        // pago: saga concluída com sucesso
    CANCELLED    // cancelado: saga falhou e compensações executadas
}
