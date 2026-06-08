package com.example.orders.api.dto;

/** DTO de saída: id do pedido criado. O checkout segue de forma assíncrona (saga). */
public record StartCheckoutResponse(String orderId) {
}
