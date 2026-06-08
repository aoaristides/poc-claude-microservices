package com.example.inventory.domain.exception;

/**
 * Exceção de domínio: tentativa de reserva quando o estoque disponível é insuficiente.
 * Sinaliza falha de negócio — não é retryable pelo consumer.
 */
public class InsufficientStockException extends RuntimeException {

    private final String sku;
    private final int available;
    private final int requested;

    public InsufficientStockException(String sku, int available, int requested) {
        super("Estoque insuficiente para SKU=%s: disponível=%d, solicitado=%d"
                .formatted(sku, available, requested));
        this.sku = sku;
        this.available = available;
        this.requested = requested;
    }

    public String getSku() {
        return sku;
    }

    public int getAvailable() {
        return available;
    }

    public int getRequested() {
        return requested;
    }
}
