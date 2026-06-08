package com.example.orders.api.dto;

/** DTO de saída da consulta de pedido: id e estado atual (DRAFT/CONFIRMED/PAID/CANCELLED). */
public record OrderStatusResponse(String orderId, String status) {
}
