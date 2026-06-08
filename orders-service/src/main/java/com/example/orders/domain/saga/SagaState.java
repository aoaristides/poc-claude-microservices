package com.example.orders.domain.saga;

/**
 * Estados da máquina da saga de checkout.
 * Os estados COMPENSATING_* representam o caminho de reversão (ordem inversa).
 */
public enum SagaState {
    AWAITING_STOCK,          // aguardando reserva de estoque
    AWAITING_PAYMENT,        // aguardando autorização de pagamento
    AWAITING_DELIVERY,       // aguardando agendamento de entrega
    COMPENSATING_PAYMENT,    // estornando pagamento (falha na entrega)
    COMPENSATING_STOCK,      // liberando reserva (falha no pagamento ou após estorno)
    COMPLETED,               // sucesso
    FAILED                   // falha com compensações concluídas
}
