package com.example.orders.application.command;

import com.example.orders.domain.model.ClientId;
import com.example.orders.domain.model.OrderItem;

import java.util.List;
import java.util.Objects;

/**
 * Comando de entrada do checkout, já em termos de domínio (VOs).
 * A tradução do DTO HTTP para este comando acontece no controller (borda).
 */
public record StartCheckoutCommand(ClientId clientId, List<OrderItem> items) {

    public StartCheckoutCommand {
        Objects.requireNonNull(clientId, "clientId");
        items = items == null ? List.of() : List.copyOf(items);
    }
}
