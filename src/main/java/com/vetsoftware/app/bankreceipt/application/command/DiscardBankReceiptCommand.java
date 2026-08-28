package com.vetsoftware.app.bankreceipt.application.command;

/**
 * Archivar una entrada que no corresponde a ningun cliente.
 *
 * <p>
 * Sin motivo escrito porque la tabla no tiene donde guardarlo. Es una carencia
 * conocida del esquema y no de este command: descartar sin dejar por que se
 * descarto obliga a quien revise el cuadre el mes siguiente a repetir el
 * trabajo. Si algun dia se añade la columna, el campo entra aqui.
 */
public record DiscardBankReceiptCommand(Long id) {
}
