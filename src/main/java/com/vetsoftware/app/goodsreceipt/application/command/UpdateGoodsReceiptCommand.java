package com.vetsoftware.app.goodsreceipt.application.command;

import java.time.LocalDate;
import java.util.List;

public record UpdateGoodsReceiptCommand(Long id, Long branchId, Long supplierId,
        Long purchaseOrderId, LocalDate receiptDate, String supplierInvoiceNumber, String notes,
        List<GoodsReceiptLineCommand> lines, Long companyId, Long updatedBy, Long version) {
}
