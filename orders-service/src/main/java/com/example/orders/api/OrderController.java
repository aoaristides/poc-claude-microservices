package com.example.orders.api;

import com.example.orders.api.dto.ItemRequest;
import com.example.orders.api.dto.OrderStatusResponse;
import com.example.orders.api.dto.StartCheckoutRequest;
import com.example.orders.api.dto.StartCheckoutResponse;
import com.example.orders.application.command.StartCheckoutCommand;
import com.example.orders.domain.model.ClientId;
import com.example.orders.domain.model.Money;
import com.example.orders.domain.model.OrderId;
import com.example.orders.domain.model.OrderItem;
import com.example.orders.domain.model.Sku;
import com.example.orders.domain.port.in.GetOrderUseCase;
import com.example.orders.domain.port.in.StartCheckoutUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * Entrada HTTP do checkout. Traduz o DTO de borda para o comando de domínio.
 * Retorna 202 Accepted: o pedido foi aceito e a saga roda de forma assíncrona.
 */
@RestController
@RequestMapping("/orders")
class OrderController {

    private final StartCheckoutUseCase startCheckout;
    private final GetOrderUseCase getOrder;

    OrderController(StartCheckoutUseCase startCheckout, GetOrderUseCase getOrder) {
        this.startCheckout = startCheckout;
        this.getOrder = getOrder;
    }

    @PostMapping
    ResponseEntity<StartCheckoutResponse> checkout(@Valid @RequestBody StartCheckoutRequest request,
                                                   UriComponentsBuilder uriBuilder) {
        var command = toCommand(request);
        OrderId orderId = startCheckout.execute(command);

        URI location = uriBuilder.path("/orders/{id}").buildAndExpand(orderId.asString()).toUri();
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .location(location)
                .body(new StartCheckoutResponse(orderId.asString()));
    }

    /** Consulta o estado atual do pedido. Usado para acompanhar o desfecho da saga. */
    @GetMapping("/{id}")
    ResponseEntity<OrderStatusResponse> status(@PathVariable String id) {
        var view = getOrder.execute(OrderId.of(id));
        return ResponseEntity.ok(
                new OrderStatusResponse(view.orderId().asString(), view.status().name()));
    }

    private StartCheckoutCommand toCommand(StartCheckoutRequest request) {
        List<OrderItem> items = request.items().stream()
                .map(this::toOrderItem)
                .toList();
        return new StartCheckoutCommand(new ClientId(request.clientId()), items);
    }

    private OrderItem toOrderItem(ItemRequest item) {
        return new OrderItem(
                new Sku(item.sku()),
                item.quantity(),
                Money.of(item.unitPrice(), item.currency()));
    }
}
