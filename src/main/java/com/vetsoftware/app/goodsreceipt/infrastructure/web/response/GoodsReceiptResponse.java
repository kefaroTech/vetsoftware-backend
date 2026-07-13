package com.vetsoftware.app.goodsreceipt.infrastructure.web.response;

import com.vetsoftware.app.goodsreceipt.domain.GoodsReceiptStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record GoodsReceiptResponse(
        Long id,
        CompanySummary company,
        BranchSummary branch,
        SupplierSummary supplier,
        Long purchaseOrderId,
        LocalDate receiptDate,
        String supplierInvoiceNumber,
        String notes,
        GoodsReceiptStatus status,
        List<GoodsReceiptLineResponse> lines,
        LocalDateTime createdDate,
        Long createdBy,
        LocalDateTime updatedDate,
        Long updatedBy,
        Long version,
        boolean enabled
) {}
