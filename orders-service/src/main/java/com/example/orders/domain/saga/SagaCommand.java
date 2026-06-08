package com.example.orders.domain.saga;

import com.example.orders.domain.model.Money;
import com.example.orders.domain.model.OrderId;

import java.util.List;
import java.util.Objects;

/**
 * Comando produzido por uma transição da saga. O domínio devolve o comando;
 * o adapter de saída traduz para o tópico/contrato Kafka correspondente.
 *
 * <p>Tipos CONFIRM_ORDER_PAYMENT e CANCEL_ORDER são "internos": o application
 * service os aplica no aggregate Order (não geram comando para outro serviço).
 */
public record SagaCommand(
        Type type,
        OrderId orderId,
        Money amount,                 // usado em AUTHORIZE_PAYMENT
        List<ReservationItem> items,  // usado em RESERVE_STOCK
        String reason                 // usado em CANCEL_ORDER
) {

    public enum Type {
        RESERVE_STOCK,
        RELEASE_RESERVATION,
        AUTHORIZE_PAYMENT,
        REFUND_PAYMENT,
        SCHEDULE_DELIVERY,
        CONFIRM_ORDER_PAYMENT,  // interno: marca o Order como PAID
        CANCEL_ORDER            // interno: cancela o Order
    }

    public SagaCommand {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(orderId, "orderId");
        items = items == null ? List.of() : List.copyOf(items);
    }

    // ---- Factories: tornam a intenção explícita e evitam construtor cheio de nulls ----

    public static SagaCommand reserveStock(OrderId orderId, List<ReservationItem> items) {
        return new SagaCommand(Type.RESERVE_STOCK, orderId, null, items, null);
    }

    public static SagaCommand releaseReservation(OrderId orderId) {
        return new SagaCommand(Type.RELEASE_RESERVATION, orderId, null, null, null);
    }

    public static SagaCommand authorizePayment(OrderId orderId, Money amount) {
        return new SagaCommand(Type.AUTHORIZE_PAYMENT, orderId, amount, null, null);
    }

    public static SagaCommand refundPayment(OrderId orderId) {
        return new SagaCommand(Type.REFUND_PAYMENT, orderId, null, null, null);
    }

    public static SagaCommand scheduleDelivery(OrderId orderId) {
        return new SagaCommand(Type.SCHEDULE_DELIVERY, orderId, null, null, null);
    }

    /** Variante com itens: o shipping precisa saber o que será entregue (enriquecida no dispatcher). */
    public static SagaCommand scheduleDelivery(OrderId orderId, List<ReservationItem> items) {
        return new SagaCommand(Type.SCHEDULE_DELIVERY, orderId, null, items, null);
    }

    public static SagaCommand confirmOrderPayment(OrderId orderId) {
        return new SagaCommand(Type.CONFIRM_ORDER_PAYMENT, orderId, null, null, null);
    }

    public static SagaCommand cancelOrder(OrderId orderId, String reason) {
        return new SagaCommand(Type.CANCEL_ORDER, orderId, null, null, reason);
    }

    /** Comandos internos são aplicados localmente no Order, não publicados a participantes. */
    public boolean isInternal() {
        return type == Type.CONFIRM_ORDER_PAYMENT || type == Type.CANCEL_ORDER;
    }
}
