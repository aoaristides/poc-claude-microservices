package com.example.orders.domain.saga;

import com.example.orders.domain.exception.InvalidTransitionException;
import com.example.orders.domain.model.Money;
import com.example.orders.domain.model.OrderId;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Aggregate da saga de checkout orquestrada.
 *
 * <p>Concentra a regra "qual o próximo passo" e "o que compensar". Cada método de
 * transição valida o estado esperado, atualiza o estado e devolve os comandos a
 * emitir. O domínio NÃO conhece Kafka — devolve {@link SagaCommand}, e o adapter
 * traduz para tópico/contrato.
 *
 * <p>O {@code require(state)} também funciona como barreira de idempotência:
 * uma resposta duplicada cai num estado que não é mais o esperado e é rejeitada.
 */
public class CheckoutSaga {

    private final SagaId id;
    private final OrderId orderId;
    private final Money totalAmount;          // necessário para emitir AUTHORIZE_PAYMENT
    private SagaState state;
    private final Set<SagaStep> completedSteps;
    private String failureReason;

    private CheckoutSaga(SagaId id, OrderId orderId, Money totalAmount,
                         SagaState state, Set<SagaStep> completedSteps, String failureReason) {
        this.id = id;
        this.orderId = orderId;
        this.totalAmount = totalAmount;
        this.state = state;
        this.completedSteps = EnumSet.noneOf(SagaStep.class);
        this.completedSteps.addAll(completedSteps);
        this.failureReason = failureReason;
    }

    /** Inicia a saga no estado AWAITING_STOCK. O primeiro comando (ReserveStock)
     *  é emitido pelo application service, que conhece os itens do pedido. */
    public static CheckoutSaga start(OrderId orderId, Money totalAmount) {
        return new CheckoutSaga(SagaId.generate(), orderId, totalAmount,
                SagaState.AWAITING_STOCK, EnumSet.noneOf(SagaStep.class), null);
    }

    /** Reconstituição a partir da persistência. */
    public static CheckoutSaga rehydrate(SagaId id, OrderId orderId, Money totalAmount,
                                         SagaState state, Set<SagaStep> completedSteps,
                                         String failureReason) {
        return new CheckoutSaga(id, orderId, totalAmount, state, completedSteps, failureReason);
    }

    // ============================ Caminho feliz ============================

    public List<SagaCommand> onStockReserved() {
        require(SagaState.AWAITING_STOCK);
        completedSteps.add(SagaStep.STOCK_RESERVED);
        state = SagaState.AWAITING_PAYMENT;
        return List.of(SagaCommand.authorizePayment(orderId, totalAmount));
    }

    public List<SagaCommand> onPaymentAuthorized() {
        require(SagaState.AWAITING_PAYMENT);
        completedSteps.add(SagaStep.PAYMENT_AUTHORIZED);
        state = SagaState.AWAITING_DELIVERY;
        return List.of(SagaCommand.scheduleDelivery(orderId));
    }

    public List<SagaCommand> onDeliveryScheduled() {
        require(SagaState.AWAITING_DELIVERY);
        completedSteps.add(SagaStep.DELIVERY_SCHEDULED);
        state = SagaState.COMPLETED;
        // comando interno: marca o Order como PAID
        return List.of(SagaCommand.confirmOrderPayment(orderId));
    }

    // ============================ Compensações ============================

    /** Falha logo na reserva: nada concluído, apenas cancela o pedido. */
    public List<SagaCommand> onStockUnavailable(String reason) {
        require(SagaState.AWAITING_STOCK);
        this.failureReason = reason;
        state = SagaState.FAILED;
        return List.of(SagaCommand.cancelOrder(orderId, reason));
    }

    /** Pagamento recusado: compensa o estoque já reservado. */
    public List<SagaCommand> onPaymentDeclined(String reason) {
        require(SagaState.AWAITING_PAYMENT);
        this.failureReason = reason;
        state = SagaState.COMPENSATING_STOCK;
        return List.of(SagaCommand.releaseReservation(orderId));
    }

    /** Falha no agendamento: começa a reverter na ordem inversa (estorna pagamento). */
    public List<SagaCommand> onDeliveryFailed(String reason) {
        require(SagaState.AWAITING_DELIVERY);
        this.failureReason = reason;
        state = SagaState.COMPENSATING_PAYMENT;
        return List.of(SagaCommand.refundPayment(orderId));
    }

    /** Pagamento estornado: segue a reversão liberando o estoque. */
    public List<SagaCommand> onPaymentRefunded() {
        require(SagaState.COMPENSATING_PAYMENT);
        state = SagaState.COMPENSATING_STOCK;
        return List.of(SagaCommand.releaseReservation(orderId));
    }

    /** Reserva liberada: fim da compensação, cancela o pedido. */
    public List<SagaCommand> onReservationReleased() {
        require(SagaState.COMPENSATING_STOCK);
        state = SagaState.FAILED;
        return List.of(SagaCommand.cancelOrder(orderId, failureReason));
    }

    // ============================ Timeout ============================

    /**
     * Transição de timeout: a saga ficou tempo demais aguardando um participante.
     * Decide a recuperação a partir do estado atual.
     *
     * <p>Política (documentada): timeout => desistir e compensar, na ordem inversa.
     * Para estados de compensação, re-emite o comando (idempotente) até o participante
     * confirmar. No AWAITING_STOCK, emite RELEASE_RESERVATION de forma defensiva — se a
     * reserva tiver ocorrido mas a resposta se perdeu, a liberação evita vazar estoque;
     * se nada foi reservado, o Inventory trata como no-op (compensação é sempre possível).
     *
     * <p>[trade-off] Sem consulta de status ao participante, há a chance de compensar um
     * passo que na verdade teve sucesso (resposta atrasada). Em produção, faça status-check
     * ou conte tentativas antes de compensar.
     */
    public List<SagaCommand> onTimeout(String reason) {
        return switch (state) {
            case AWAITING_STOCK, AWAITING_PAYMENT -> {
                this.failureReason = reason;
                state = SagaState.COMPENSATING_STOCK;
                yield List.of(SagaCommand.releaseReservation(orderId));
            }
            case AWAITING_DELIVERY -> {
                this.failureReason = reason;
                state = SagaState.COMPENSATING_PAYMENT;
                yield List.of(SagaCommand.refundPayment(orderId));
            }
            // Estados de compensação: re-emite o comando idempotente (re-drive).
            case COMPENSATING_PAYMENT -> List.of(SagaCommand.refundPayment(orderId));
            case COMPENSATING_STOCK -> List.of(SagaCommand.releaseReservation(orderId));
            // Terminais: nada a fazer (o scheduler nem deveria selecioná-los).
            case COMPLETED, FAILED -> List.of();
        };
    }

    // ============================ Dispatcher ============================

    /**
     * Aplica a transição correspondente ao tipo da resposta. Centraliza o switch
     * para o application service não precisar conhecer o mapeamento.
     */
    public List<SagaCommand> apply(SagaReply reply) {
        return switch (reply.type()) {
            case STOCK_RESERVED       -> onStockReserved();
            case STOCK_UNAVAILABLE    -> onStockUnavailable(reply.reason());
            case PAYMENT_AUTHORIZED   -> onPaymentAuthorized();
            case PAYMENT_DECLINED     -> onPaymentDeclined(reply.reason());
            case PAYMENT_REFUNDED     -> onPaymentRefunded();
            case DELIVERY_SCHEDULED   -> onDeliveryScheduled();
            case DELIVERY_FAILED      -> onDeliveryFailed(reply.reason());
            case RESERVATION_RELEASED -> onReservationReleased();
        };
    }

    private void require(SagaState expected) {
        if (state != expected) {
            throw new InvalidTransitionException(
                    "saga em %s, esperado %s".formatted(state, expected));
        }
    }

    public boolean isFinished() {
        return state == SagaState.COMPLETED || state == SagaState.FAILED;
    }

    // ---- getters para o adapter de persistência ----

    public SagaId id() {
        return id;
    }

    public OrderId orderId() {
        return orderId;
    }

    public Money totalAmount() {
        return totalAmount;
    }

    public SagaState state() {
        return state;
    }

    public Set<SagaStep> completedSteps() {
        // Cópia defensiva segura mesmo quando vazio (EnumSet.copyOf(Collection) lança se vazio).
        var copy = EnumSet.noneOf(SagaStep.class);
        copy.addAll(completedSteps);
        return copy;
    }

    public String failureReason() {
        return failureReason;
    }
}
