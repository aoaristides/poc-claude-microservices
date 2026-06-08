package com.example.payment.domain.model;

/**
 * Resultado da política de autorização de pagamento.
 * Sealed para garantir que todos os casos sejam tratados via pattern matching.
 */
public sealed interface AuthorizationResult {

    /** Pagamento aprovado. */
    record Approved() implements AuthorizationResult {}

    /** Pagamento recusado com razão descritiva. */
    record Declined(String reason) implements AuthorizationResult {}
}
