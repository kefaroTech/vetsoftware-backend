package com.vetsoftware.app.purchaseorder.infrastructure.web.response;

import com.vetsoftware.app.purchaseorder.domain.PurchaseOrderStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record PurchaseOrderResponse(Long id, CompanySummary company, BranchSummary branch,
        SupplierSummary supplier, PurchaseOrderStatus status, LocalDate orderDate,
        LocalDate expectedDate, String notes, List<PurchaseOrderLineResponse> lines,
        LocalDateTime createdDate, Long createdBy, LocalDateTime updatedDate, Long updatedBy,
        Long version, boolean enabled) {
}
