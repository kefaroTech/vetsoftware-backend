package com.vetsoftware.app.purchaseorder.infrastructure.web.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

public record CreatePurchaseOrderRequest(
        @NotNull Long branchId,
        @NotNull Long supplierId,
        @NotNull LocalDate orderDate,
        LocalDate expectedDate,
        @Size(max = 500) String notes,
        @NotEmpty @Valid List<PurchaseOrderLineRequest> lines
) {}
