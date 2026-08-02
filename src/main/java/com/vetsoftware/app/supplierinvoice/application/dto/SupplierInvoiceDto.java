package com.vetsoftware.app.supplierinvoice.application.dto;

import com.vetsoftware.app.supplierinvoice.domain.SupplierInvoice;
import com.vetsoftware.app.supplierinvoice.domain.SupplierInvoiceStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record SupplierInvoiceDto(
    Long id,
    CompanySummaryDto company,
    BranchSummaryDto branch,
    SupplierSummaryDto supplier,
    Long purchaseOrderId,
    Long goodsReceiptId,
    String invoiceNumber,
    LocalDate issueDate,
    LocalDate dueDate,
    BigDecimal subtotal,
    BigDecimal taxAmount,
    BigDecimal withholdingAmount,
    BigDecimal total,
    BigDecimal payableAmount,
    BigDecimal paidAmount,
    BigDecimal balance,
    SupplierInvoiceStatus status,
    String notes,
    List<SupplierInvoicePaymentDto> payments,
    LocalDateTime createdDate,
    Long createdBy,
    LocalDateTime updatedDate,
    Long updatedBy,
    Long version,
    boolean enabled) {
  public static SupplierInvoiceDto from(SupplierInvoice inv) {
    return new SupplierInvoiceDto(
        inv.getId(),
        CompanySummaryDto.from(inv.getCompany()),
        BranchSummaryDto.from(inv.getBranch()),
        SupplierSummaryDto.from(inv.getSupplier()),
        inv.getPurchaseOrderId(),
        inv.getGoodsReceiptId(),
        inv.getInvoiceNumber(),
        inv.getIssueDate(),
        inv.getDueDate(),
        inv.getSubtotal(),
        inv.getTaxAmount(),
        inv.getWithholdingAmount(),
        inv.getTotal(),
        inv.payableAmount(),
        inv.paidAmount(),
        inv.balance(),
        inv.getStatus(),
        inv.getNotes(),
        inv.getPayments().stream().map(SupplierInvoicePaymentDto::from).toList(),
        inv.getCreatedDate(),
        inv.getCreatedBy(),
        inv.getUpdatedDate(),
        inv.getUpdatedBy(),
        inv.getVersion(),
        inv.isEnabled());
  }
}
