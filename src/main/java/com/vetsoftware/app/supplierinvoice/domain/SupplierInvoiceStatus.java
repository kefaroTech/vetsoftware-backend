package com.vetsoftware.app.supplierinvoice.domain;

/**
 * Estado de la factura de proveedor / cuenta por pagar.
 *
 * <ul>
 *   <li>{@code PENDING}: sin abonos (saldo = total por pagar).
 *   <li>{@code PARTIAL}: abonada parcialmente (0 &lt; abonado &lt; total por pagar).
 *   <li>{@code PAID}: saldada por completo.
 *   <li>{@code CANCELLED}: anulada (solo posible sin abonos).
 * </ul>
 */
public enum SupplierInvoiceStatus {
  PENDING,
  PARTIAL,
  PAID,
  CANCELLED
}
