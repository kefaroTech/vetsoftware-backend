package com.vetsoftware.app.paymentattempt.domain;

/**
 * Se agotaron los intentos que la red permite gastar contra el cliente dentro
 * de la ventana ({@link PaymentAttempt#MAX_SOFT_ATTEMPTS} en
 * {@link PaymentAttempt#RETRY_WINDOW}).
 *
 * <p>
 * Conflicto (409) y no error de datos: el cobro está bien formado, lo que no
 * queda es presupuesto de reintento. Solo cuentan los intentos que
 * {@link PaymentAttempt#consumesCustomerAttempts()} declara imputables — un
 * fallo propio de configuración no gasta presupuesto de nadie.
 */
public class RetryBudgetExhaustedException extends RuntimeException {

    public RetryBudgetExhaustedException(Long billingDocumentId, int maxAttempts) {
        super("Retry budget exhausted for billing document " + billingDocumentId + ": "
                + maxAttempts + " chargeable attempts already spent in the window");
    }
}
