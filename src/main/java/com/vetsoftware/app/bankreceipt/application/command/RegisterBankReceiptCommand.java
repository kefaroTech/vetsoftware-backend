package com.vetsoftware.app.bankreceipt.application.command;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Alta de una linea del extracto.
 *
 * <p>
 * <strong>No lleva {@code companyId} y no hay ninguno que pudiera
 * llevar.</strong> La tabla no tiene empresa: antes de identificar la entrada
 * no se sabe de quien es, y averiguarlo es el trabajo que esta feature
 * organiza. No es la omision defensiva de un recurso scoped —aqui no hay nada
 * que suplantar—, es que el dato no existe todavia.
 *
 * @param amount
 *            con signo. Un negativo es un cargo o una devolucion del banco y es
 *            tan valido como una consignacion; lo unico prohibido es el cero
 */
public record RegisterBankReceiptCommand(String bankAccountRef, String bankReference,
        LocalDate receivedOn, BigDecimal amount, String description) {
}
