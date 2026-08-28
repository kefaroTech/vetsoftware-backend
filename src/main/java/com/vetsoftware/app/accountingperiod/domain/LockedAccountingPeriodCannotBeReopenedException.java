package com.vetsoftware.app.accountingperiod.domain;

/**
 * Un periodo {@code LOCKED} no se reabre. Nunca, y no hay parametro que lo
 * permita.
 *
 * <p>
 * <strong>Es la unica regla de esta ficha que no admite excepcion
 * escrita</strong>, y por eso tiene excepcion propia en vez de compartir la de
 * «ya cerrado». {@code SOFT_CLOSED} significa «cerrado y todavia corregible con
 * firma»; {@code LOCKED} significa «declarado», es decir que el numero ya salio
 * de la empresa hacia una autoridad o hacia un tercero. Reabrirlo no seria
 * corregir un error: seria dejar la base contando algo distinto de lo que se
 * declaro, sin que nada en el sistema recuerde cual de los dos numeros se
 * envio.
 *
 * <p>
 * Lo que si existe para ese caso es abrir un periodo posterior e imputar alli
 * el ajuste, que es exactamente lo que hace
 * {@code ResolvePostingPeriodUseCase}. Mapea a 409.
 */
public class LockedAccountingPeriodCannotBeReopenedException extends RuntimeException {

    public LockedAccountingPeriodCannotBeReopenedException(Long id) {
        super("Accounting period " + id + " is locked and cannot be reopened");
    }
}
