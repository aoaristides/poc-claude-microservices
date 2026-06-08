package com.example.orders.infrastructure.messaging.contract;

import java.util.List;

/**
 * Contrato publicado do comando ScheduleDelivery (Published Language).
 *
 * <p>Carrega os itens do pedido: o shipping precisa saber <em>o que</em> será entregue
 * para decidir a entregabilidade (ex.: SKU não atendido pela transportadora). Separado
 * dos objetos de domínio para que a evolução do schema não vaze para o domínio.
 */
public record ScheduleDeliveryMessage(String orderId, List<Item> items) {

    public record Item(String sku, int quantity) {
    }
}
