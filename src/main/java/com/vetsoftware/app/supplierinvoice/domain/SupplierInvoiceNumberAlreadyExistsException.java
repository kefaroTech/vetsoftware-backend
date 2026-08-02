package com.vetsoftware.app.supplierinvoice.domain;

/**
 * Ya existe una factura activa con ese número para ese proveedor en la empresa
 * (evita doble registro).
 */
public class SupplierInvoiceNumberAlreadyExistsException extends RuntimeException {
    public SupplierInvoiceNumberAlreadyExistsException(String invoiceNumber) {
        super("A supplier invoice with number '" + invoiceNumber
                + "' already exists for this supplier");
    }
}
