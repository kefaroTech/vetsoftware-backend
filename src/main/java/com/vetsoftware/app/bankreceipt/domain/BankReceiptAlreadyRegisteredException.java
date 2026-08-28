package com.vetsoftware.app.bankreceipt.domain;

import java.time.LocalDate;

/**
 * Ya hay una entrada con esa referencia bancaria en esa misma fecha.
 *
 * <p>
 * Es el espejo en Java de {@code uq_bank_receipts_reference}. Existe para que
 * recargar dos veces el mismo extracto del mes —el error mas comun de este
 * proceso, porque el fichero se descarga a mano— conteste un conflicto legible
 * en vez de un 500 con un {@code Duplicate entry} del driver.
 *
 * <p>
 * <strong>La referencia se compara EXACTO</strong>, igual que la columna
 * ({@code ascii_bin}): {@code AB12} y {@code ab12} son entradas distintas y las
 * dos entran. Es un identificador que escribe el banco, no un texto de una
 * persona, y tratarlas como iguales descartaria como duplicada la segunda
 * consignacion del dia.
 */
public class BankReceiptAlreadyRegisteredException extends RuntimeException {

    public BankReceiptAlreadyRegisteredException(String bankReference, LocalDate receivedOn) {
        super("Bank receipt already registered for reference " + bankReference + " on "
                + receivedOn);
    }
}
