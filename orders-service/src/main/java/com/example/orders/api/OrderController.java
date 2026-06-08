package com.example.orders.api;

import com.example.orders.api.dto.ItemRequest;
import com.example.orders.api.dto.StartCheckoutRequest;
import com.example.orders.api.dto.StartCheckoutResponse;
import com.example.orders.application.command.StartCheckoutCommand;
import com.example.orders.domain.model.ClientId;
import com.example.orders.domain.model.Money;
import com.example.orders.domain.model.OrderId;
import com.example.orders.domain.model.OrderItem;
import com.example.orders.domain.model.Sku;
import com.example.orders.domain.port.in.StartCheckoutUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    OrderController(StartCheckoutUseCase startCheckout) {
        this.startCheckout = startCheckout;
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
