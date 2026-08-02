package com.vetsoftware.app.supplierinvoice.infrastructure.web.response;

import com.vetsoftware.app.supplierinvoice.domain.SupplierInvoiceStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record SupplierInvoiceResponse(Long id, CompanySummary company, BranchSummary branch,
        SupplierSummary supplier, Long purchaseOrderId, Long goodsReceiptId, String invoiceNumber,
        LocalDate issueDate, LocalDate dueDate, BigDecimal subtotal, BigDecimal taxAmount,
        BigDecimal withholdingAmount, BigDecimal total, BigDecimal payableAmount,
        BigDecimal paidAmount, BigDecimal balance, SupplierInvoiceStatus status, String notes,
        List<SupplierInvoicePaymentResponse> payments, LocalDateTime createdDate, Long createdBy,
        LocalDateTime updatedDate, Long updatedBy, Long version, boolean enabled) {
}
