package com.vetsoftware.app.goodsreceipt.application.dto;

import com.vetsoftware.app.goodsreceipt.domain.GoodsReceipt;
import com.vetsoftware.app.goodsreceipt.domain.GoodsReceiptStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record GoodsReceiptDto(
    Long id,
    CompanySummaryDto company,
    BranchSummaryDto branch,
    SupplierSummaryDto supplier,
    Long purchaseOrderId,
    LocalDate receiptDate,
    String supplierInvoiceNumber,
    String notes,
    GoodsReceiptStatus status,
    List<GoodsReceiptLineDto> lines,
    LocalDateTime createdDate,
    Long createdBy,
    LocalDateTime updatedDate,
    Long updatedBy,
    Long version,
    boolean enabled) {
  public static GoodsReceiptDto from(GoodsReceipt receipt) {
    return new GoodsReceiptDto(
        receipt.getId(),
        CompanySummaryDto.from(receipt.getCompany()),
        BranchSummaryDto.from(receipt.getBranch()),
        SupplierSummaryDto.from(receipt.getSupplier()),
        receipt.getPurchaseOrderId(),
        receipt.getReceiptDate(),
        receipt.getSupplierInvoiceNumber(),
        receipt.getNotes(),
        receipt.getStatus(),
        receipt.getLines().stream().map(GoodsReceiptLineDto::from).toList(),
        receipt.getCreatedDate(),
        receipt.getCreatedBy(),
        receipt.getUpdatedDate(),
        receipt.getUpdatedBy(),
        receipt.getVersion(),
        receipt.isEnabled());
  }
}
