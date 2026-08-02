package com.vetsoftware.app.goodsreceipt.infrastructure.web.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

public record CreateGoodsReceiptRequest(
    @NotNull Long branchId,
    @NotNull Long supplierId,
    Long purchaseOrderId,
    @NotNull LocalDate receiptDate,
    @Size(max = 60) String supplierInvoiceNumber,
    @Size(max = 500) String notes,
    @NotEmpty @Valid List<GoodsReceiptLineRequest> lines) {}
