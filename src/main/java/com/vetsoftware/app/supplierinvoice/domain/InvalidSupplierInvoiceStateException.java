package com.vetsoftware.app.supplierinvoice.domain;

/**
 * Operación no válida para el estado actual de la factura (editar/anular con
 * abonos, abonar una anulada/pagada, sobrepago).
 */
public class InvalidSupplierInvoiceStateException extends RuntimeException {
    public InvalidSupplierInvoiceStateException(String message) {
        super(message);
    }
}
