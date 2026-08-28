package com.vetsoftware.app.gatewaysettlement.application.command;

/**
 * Ata el lote a la linea del extracto por la que entro su neto: la ultima milla
 * de la conciliacion bancaria.
 */
public record LinkBankReceiptCommand(Long id, Long bankReceiptId) {
}
