package com.vetsoftware.app.supplierinvoice.domain;

/**
 * Estado de la factura de proveedor / cuenta por pagar.
 * <ul>
 *   <li>{@code PENDING}: sin abonos (saldo = total por pagar).</li>
 *   <li>{@code PARTIAL}: abonada parcialmente (0 &lt; abonado &lt; total por pagar).</li>
 *   <li>{@code PAID}: saldada por completo.</li>
 *   <li>{@code CANCELLED}: anulada (solo posible sin abonos).</li>
 * </ul>
 */
public enum SupplierInvoiceStatus {
    PENDING, PARTIAL, PAID, CANCELLED
}
