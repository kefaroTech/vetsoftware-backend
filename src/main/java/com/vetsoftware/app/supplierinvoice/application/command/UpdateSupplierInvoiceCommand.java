package com.vetsoftware.app.supplierinvoice.application.command;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UpdateSupplierInvoiceCommand(Long id, Long branchId, Long supplierId,
        Long purchaseOrderId, Long goodsReceiptId, String invoiceNumber, LocalDate issueDate,
        LocalDate dueDate, BigDecimal subtotal, BigDecimal taxAmount, BigDecimal withholdingAmount,
        String notes, Long companyId, Long actorId, Long version) {
}
