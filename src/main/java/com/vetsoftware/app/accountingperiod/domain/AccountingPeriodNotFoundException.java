package com.vetsoftware.app.accountingperiod.domain;

/**
 * El periodo pedido no existe. Mapea a 404.
 *
 * <p>
 * Tiene dos constructores porque esta ficha se busca de las dos maneras: por
 * {@code id} —lo que escribe el cliente en la URL— y por clave de mes, que es
 * como la nombra el resto del sistema ({@code posting_period} es la clave, no
 * el id). Un mensaje que dijera «not found: 42» cuando lo que falta es
 * {@code 2026-03} manda a quien lo lee a buscar la fila equivocada.
 */
public class AccountingPeriodNotFoundException extends RuntimeException {

    public AccountingPeriodNotFoundException(Long id) {
        super("Accounting period not found: " + id);
    }

    public AccountingPeriodNotFoundException(AccountingPeriodKey periodKey) {
        super("Accounting period not found: " + periodKey);
    }
}
