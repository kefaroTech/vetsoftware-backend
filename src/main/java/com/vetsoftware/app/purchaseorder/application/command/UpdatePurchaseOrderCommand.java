package com.vetsoftware.app.purchaseorder.application.command;

import java.time.LocalDate;
import java.util.List;

public record UpdatePurchaseOrderCommand(Long id, Long branchId, Long supplierId,
        LocalDate orderDate, LocalDate expectedDate, String notes,
        List<PurchaseOrderLineCommand> lines, Long companyId, Long updatedBy, Long version) {
}
