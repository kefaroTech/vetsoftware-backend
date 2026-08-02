package com.vetsoftware.app.purchaseorder.application.command;

import com.vetsoftware.app.purchaseorder.domain.PurchaseOrderStatus;
import java.time.LocalDate;

public record SearchPurchaseOrdersCommand(
    Long companyId,
    Long supplierId,
    Long branchId,
    PurchaseOrderStatus status,
    LocalDate from,
    LocalDate to,
    int page,
    int pageSize) {}
