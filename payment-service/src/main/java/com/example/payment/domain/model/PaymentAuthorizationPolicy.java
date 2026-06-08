package com.example.payment.domain.model;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Set;

/**
 * Domain Service: política de autorização do gateway simulado.
 *
 * <p>Encapsula as regras de decisão — aprovação/recusa — isolando o aggregate
 * de detalhes de configuração. É determinístico para facilitar testes.
 *
 * <p>Regras:
 * <ol>
 *   <li>A moeda precisa ser suportada (apenas BRL).</li>
 *   <li>O valor não pode exceder o limite configurado.</li>
 * </ol>
 */
public class PaymentAuthorizationPolicy {

    private static final Set<Currency> SUPPORTED_CURRENCIES =
            Set.of(Currency.getInstance("BRL"));

    private final BigDecimal maxAmount;

    public PaymentAuthorizationPolicy(BigDecimal maxAmount) {
        if (maxAmount == null || maxAmount.signum() <= 0) {
            throw new IllegalArgumentException("maxAmount precisa ser positivo");
        }
        this.maxAmount = maxAmount;
    }

    /**
     * Avalia se o pagamento deve ser aprovado ou recusado.
     *
     * @param money valor e moeda a ser autorizado
     * @return {@link AuthorizationResult.Approved} ou {@link AuthorizationResult.Declined}
     */
    public AuthorizationResult evaluate(Money money) {
        if (!SUPPORTED_CURRENCIES.contains(money.currency())) {
            return new AuthorizationResult.Declined(
                    "unsupported currency: " + money.currency().getCurrencyCode());
        }
        if (money.isGreaterThan(maxAmount)) {
            return new AuthorizationResult.Declined(
                    "amount exceeds limit of " + maxAmount.toPlainString());
        }
        return new AuthorizationResult.Approved();
    }
}
