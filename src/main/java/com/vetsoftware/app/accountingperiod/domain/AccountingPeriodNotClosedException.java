package com.vetsoftware.app.accountingperiod.domain;

/**
 * Se intento reabrir un periodo que no estaba cerrado.
 *
 * <p>
 * Parece un caso imposible desde la consola —nadie pulsa «reabrir» sobre un mes
 * que ya esta abierto— y por eso merece nombre propio: llega cuando dos
 * personas atienden el mismo cierre y la segunda trabaja sobre una lista que se
 * le quedo vieja en pantalla. Contestar aqui un
 * {@link AccountingPeriodAlreadyClosedException} diria justo lo contrario de lo
 * que pasa y mandaria a quien lo lee a buscar el problema donde no esta.
 *
 * <p>
 * Mapea a 409.
 */
public class AccountingPeriodNotClosedException extends RuntimeException {

    public AccountingPeriodNotClosedException(Long id, AccountingPeriodStatus status) {
        super("Accounting period " + id + " is not closed, its status is " + status);
    }
}
