package com.vetsoftware.app.accountingexport.domain;

/**
 * Se intento dar desenlace a una exportacion que ya lo tenia.
 *
 * <p>
 * Es un conflicto (409): el cuerpo es valido y lo que falla es el estado del
 * fichero en este instante.
 *
 * <p>
 * <strong>La base impide la fila incoherente, no la transicion
 * equivocada.</strong> {@code chk_accounting_exports_lifecycle} comprueba que
 * un {@code REJECTED} lleve fecha y motivo, pero no sabe de donde venia: pasar
 * de {@code DELIVERED} a {@code REJECTED} produce una fila que el {@code CHECK}
 * acepta sin una queja. Lo que se pierde ahi no es un dato cualquiera —es la
 * fecha de entrega, borrada por un rechazo que llego despues— y con ella la
 * prueba de que el mes se entrego a tiempo.
 */
public class AccountingExportAlreadyResolvedException extends RuntimeException {

    public AccountingExportAlreadyResolvedException(Long id, AccountingExportStatus status) {
        super("Accounting export " + id + " is already resolved with status " + status);
    }
}
