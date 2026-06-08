package com.example.orders.domain.saga;

import com.example.orders.domain.exception.InvalidTransitionException;
import com.example.orders.domain.model.Money;
import com.example.orders.domain.model.OrderId;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Testa a máquina de estados da saga: caminho feliz, compensações e barreira de idempotência. */
class CheckoutSagaTest {

    private static CheckoutSaga newSaga() {
        return CheckoutSaga.start(OrderId.generate(), Money.of(new BigDecimal("100.00"), "BRL"));
    }

    @Test
    void caminho_feliz_avanca_ate_concluir() {
        var saga = newSaga();

        assertThat(first(saga.onStockReserved()).type()).isEqualTo(SagaCommand.Type.AUTHORIZE_PAYMENT);
        assertThat(saga.state()).isEqualTo(SagaState.AWAITING_PAYMENT);

        assertThat(first(saga.onPaymentAuthorized()).type()).isEqualTo(SagaCommand.Type.SCHEDULE_DELIVERY);
        assertThat(saga.state()).isEqualTo(SagaState.AWAITING_DELIVERY);

        assertThat(first(saga.onDeliveryScheduled()).type()).isEqualTo(SagaCommand.Type.CONFIRM_ORDER_PAYMENT);
        assertThat(saga.state()).isEqualTo(SagaState.COMPLETED);
        assertThat(saga.isFinished()).isTrue();
    }

    @Test
    void autorizar_pagamento_carrega_o_valor_total() {
        var saga = CheckoutSaga.start(OrderId.generate(), Money.of(new BigDecimal("42.00"), "BRL"));

        var command = first(saga.onStockReserved());

        assertThat(command.amount()).isEqualTo(Money.of(new BigDecimal("42.00"), "BRL"));
    }

    @Test
    void pagamento_recusado_compensa_estoque() {
        var saga = newSaga();
        saga.onStockReserved();

        var releaseCmds = saga.onPaymentDeclined("cartão sem saldo");

        assertThat(first(releaseCmds).type()).isEqualTo(SagaCommand.Type.RELEASE_RESERVATION);
        assertThat(saga.state()).isEqualTo(SagaState.COMPENSATING_STOCK);

        var cancelCmds = saga.onReservationReleased();
        assertThat(first(cancelCmds).type()).isEqualTo(SagaCommand.Type.CANCEL_ORDER);
        assertThat(first(cancelCmds).reason()).isEqualTo("cartão sem saldo");
        assertThat(saga.state()).isEqualTo(SagaState.FAILED);
    }

    @Test
    void falha_no_agendamento_compensa_na_ordem_inversa() {
        var saga = newSaga();
        saga.onStockReserved();
        saga.onPaymentAuthorized();

        assertThat(first(saga.onDeliveryFailed("sem cobertura")).type())
                .isEqualTo(SagaCommand.Type.REFUND_PAYMENT);
        assertThat(saga.state()).isEqualTo(SagaState.COMPENSATING_PAYMENT);

        assertThat(first(saga.onPaymentRefunded()).type())
                .isEqualTo(SagaCommand.Type.RELEASE_RESERVATION);
        assertThat(saga.state()).isEqualTo(SagaState.COMPENSATING_STOCK);

        assertThat(first(saga.onReservationReleased()).type())
                .isEqualTo(SagaCommand.Type.CANCEL_ORDER);
        assertThat(saga.state()).isEqualTo(SagaState.FAILED);
    }

    @Test
    void estoque_indisponivel_no_inicio_cancela_sem_compensar() {
        var saga = newSaga();

        var cmds = saga.onStockUnavailable("sem estoque");

        assertThat(first(cmds).type()).isEqualTo(SagaCommand.Type.CANCEL_ORDER);
        assertThat(saga.state()).isEqualTo(SagaState.FAILED);
    }

    @Test
    void transicao_invalida_e_rejeitada_barreira_de_idempotencia() {
        var saga = newSaga(); // estado AWAITING_STOCK

        // resposta de pagamento chegando fora de hora (ex.: duplicada/atrasada)
        assertThatThrownBy(saga::onPaymentAuthorized)
                .isInstanceOf(InvalidTransitionException.class);
    }

    @Test
    void timeout_aguardando_estoque_libera_reserva_defensivamente() {
        var saga = newSaga(); // AWAITING_STOCK

        var cmds = saga.onTimeout("timeout");

        assertThat(first(cmds).type()).isEqualTo(SagaCommand.Type.RELEASE_RESERVATION);
        assertThat(saga.state()).isEqualTo(SagaState.COMPENSATING_STOCK);
    }

    @Test
    void timeout_aguardando_pagamento_compensa_estoque() {
        var saga = newSaga();
        saga.onStockReserved(); // AWAITING_PAYMENT

        var cmds = saga.onTimeout("timeout");

        assertThat(first(cmds).type()).isEqualTo(SagaCommand.Type.RELEASE_RESERVATION);
        assertThat(saga.state()).isEqualTo(SagaState.COMPENSATING_STOCK);
    }

    @Test
    void timeout_aguardando_entrega_estorna_pagamento() {
        var saga = newSaga();
        saga.onStockReserved();
        saga.onPaymentAuthorized(); // AWAITING_DELIVERY

        var cmds = saga.onTimeout("timeout");

        assertThat(first(cmds).type()).isEqualTo(SagaCommand.Type.REFUND_PAYMENT);
        assertThat(saga.state()).isEqualTo(SagaState.COMPENSATING_PAYMENT);
    }

    @Test
    void timeout_em_compensacao_reemite_comando_idempotente_sem_mudar_estado() {
        var saga = newSaga();
        saga.onStockReserved();
        saga.onPaymentDeclined("recusado"); // COMPENSATING_STOCK

        var cmds = saga.onTimeout("timeout");

        assertThat(first(cmds).type()).isEqualTo(SagaCommand.Type.RELEASE_RESERVATION);
        assertThat(saga.state()).isEqualTo(SagaState.COMPENSATING_STOCK); // re-drive, mesmo estado
    }

    @Test
    void apply_despacha_para_a_transicao_correta() {
        var saga = newSaga();

        var cmds = saga.apply(SagaReply.of(saga.orderId(), SagaReplyType.STOCK_RESERVED));

        assertThat(first(cmds).type()).isEqualTo(SagaCommand.Type.AUTHORIZE_PAYMENT);
    }

    private static SagaCommand first(java.util.List<SagaCommand> commands) {
        return commands.get(0);
    }
}
