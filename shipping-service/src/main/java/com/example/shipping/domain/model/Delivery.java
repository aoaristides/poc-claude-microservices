package com.example.shipping.domain.model;

import com.example.shipping.domain.exception.DeliveryAlreadyScheduledException;

/**
 * Aggregate raiz de entrega.
 *
 * <p>Protege a invariante de transição de estado: uma entrega é imutável após
 * {@link #schedule(DeliveryStatus, String, String)}. Re-chamar schedule numa entrega
 * já processada lança {@link DeliveryAlreadyScheduledException}.
 *
 * <p>O trackingCode é determinístico: prefixo fixo + primeiros 8 chars do orderId,
 * garantindo reprodutibilidade em testes sem aleatoriedade.
 */
public class Delivery {

    private static final String TRACKING_PREFIX = "TRK-";

    private final String orderId;
    private DeliveryStatus status;
    private String trackingCode;
    private String reason;

    /** Construtor de reconstituição (carregado do banco via adapter). */
    private Delivery(String orderId, DeliveryStatus status, String trackingCode, String reason) {
        this.orderId = orderId;
        this.status = status;
        this.trackingCode = trackingCode;
        this.reason = reason;
    }

    /** Cria uma entrega nova, ainda sem estado definido. */
    public static Delivery newDelivery(String orderId) {
        return new Delivery(orderId, null, null, null);
    }

    /** Reconstitui a partir do estado persistido. */
    public static Delivery reconstitute(String orderId, DeliveryStatus status,
                                        String trackingCode, String reason) {
        return new Delivery(orderId, status, trackingCode, reason);
    }

    /**
     * Executa o agendamento da entrega.
     *
     * <p>Se já foi processada, lança exceção — idempotência de negócio é tratada
     * ANTES de chegar aqui (no application service), mas o aggregate protege
     * por segurança.
     *
     * @param status       SCHEDULED ou FAILED
     * @param trackingCode código de rastreio (ignorado quando FAILED)
     * @param reason       motivo da falha (null quando SCHEDULED)
     */
    public void schedule(DeliveryStatus status, String trackingCode, String reason) {
        if (this.status != null) {
            throw new DeliveryAlreadyScheduledException(
                    "entrega já processada para orderId=" + orderId + " status=" + this.status);
        }
        this.status = status;
        this.trackingCode = trackingCode;
        this.reason = reason;
    }

    /**
     * Gera o trackingCode determinístico a partir do orderId.
     *
     * <p>Usa os primeiros 8 caracteres do UUID (sem hífens) para manter o código
     * curto e reprodutível. Ex.: orderId="550e8400-e29b-41d4-..." → "TRK-550e8400".
     */
    public static String generateTrackingCode(String orderId) {
        String stripped = orderId.replace("-", "");
        String suffix = stripped.substring(0, Math.min(8, stripped.length())).toUpperCase();
        return TRACKING_PREFIX + suffix;
    }

    public String getOrderId() {
        return orderId;
    }

    public DeliveryStatus getStatus() {
        return status;
    }

    public String getTrackingCode() {
        return trackingCode;
    }

    public String getReason() {
        return reason;
    }
}
