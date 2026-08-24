package com.vetsoftware.app.subscriptionpayment.application.port.out;

/**
 * R4: el saldo de una factura es siempre su total menos lo aplicado confirmado.
 *
 * <p>
 * <strong>Lo que la base SI garantiza:</strong>
 * {@code balance_amount = total_amount - settled_amount}, porque
 * {@code balance_amount} es una columna calculada y ningun camino de codigo
 * puede desincronizarla. Este slice <strong>no la escribe nunca</strong>.
 *
 * <p>
 * <strong>Lo que la base NO garantiza y es esta regla:</strong> que
 * {@code settled_amount} sea de verdad la suma de las aplicaciones confirmadas.
 * Esa columna si la escribe el codigo, y es la que decide si una cuenta entra
 * en mora: un camino capaz de desincronizarla es un camino capaz de suspender a
 * quien ya pago.
 *
 * <p>
 * Por eso se <strong>recalcula</strong> desde las aplicaciones dentro de la
 * misma transaccion, y nunca se acumula con
 * {@code settledAmount = settledAmount + x} desde Java: un acumulador pierde la
 * reconciliacion en cuanto un paso falla a medias, y el recalculo no.
 */
public interface BillingDocumentSettlementPort {

    /**
     * Recalcula {@code settled_amount} del documento como la suma de sus
     * aplicaciones cuyo origen cuenta como cobro: las de nota credito siempre, y
     * las de pago solo si el pago esta {@code CONFIRMED}.
     *
     * @return filas actualizadas: 0 significa que el documento no existe o no es de
     *         esa empresa
     */
    int recalculateSettledAmount(Long documentId, Long companyId);
}
