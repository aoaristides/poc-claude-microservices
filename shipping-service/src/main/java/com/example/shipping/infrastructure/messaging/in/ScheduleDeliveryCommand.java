package com.example.shipping.infrastructure.messaging.in;

import java.util.List;

/**
 * Comando de entrada já traduzido pela ACL: o que o domínio precisa para agendar.
 * Apenas {@code orderId} e os {@code skus} do pedido (o shipping não precisa de preço).
 */
record ScheduleDeliveryCommand(String orderId, List<String> skus) {
}
