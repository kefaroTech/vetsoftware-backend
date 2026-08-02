package com.vetsoftware.app.purchasereport.application.port.out;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Lee las facturas de proveedor (feature {@code supplierinvoice}) del periodo
 * para el libro de compras. Convierte el enum de estado ajeno a String en el
 * borde para no acoplar {@code
 * purchasereport} a ese dominio.
 */
public interface PurchaseDocumentQueryPort {
    List<PurchaseDocumentView> findByCompanyAndDateRange(Long companyId, LocalDate from,
            LocalDate to, Long branchId);

    record PurchaseDocumentView(Long id, String supplierName, String supplierTaxId,
            String invoiceNumber, LocalDate issueDate, LocalDate dueDate, BigDecimal subtotal,
            BigDecimal taxAmount, BigDecimal withholdingAmount, BigDecimal total,
            BigDecimal paidAmount, BigDecimal balance, String status) {
    }
}
