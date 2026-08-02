package com.vetsoftware.app.purchaseorder.application.dto;

import com.vetsoftware.app.purchaseorder.domain.PurchaseOrder;
import com.vetsoftware.app.purchaseorder.domain.PurchaseOrderStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record PurchaseOrderDto(
    Long id,
    CompanySummaryDto company,
    BranchSummaryDto branch,
    SupplierSummaryDto supplier,
    PurchaseOrderStatus status,
    LocalDate orderDate,
    LocalDate expectedDate,
    String notes,
    List<PurchaseOrderLineDto> lines,
    LocalDateTime createdDate,
    Long createdBy,
    LocalDateTime updatedDate,
    Long updatedBy,
    Long version,
    boolean enabled) {
  public static PurchaseOrderDto from(PurchaseOrder order) {
    return new PurchaseOrderDto(
        order.getId(),
        CompanySummaryDto.from(order.getCompany()),
        BranchSummaryDto.from(order.getBranch()),
        SupplierSummaryDto.from(order.getSupplier()),
        order.getStatus(),
        order.getOrderDate(),
        order.getExpectedDate(),
        order.getNotes(),
        order.getLines().stream().map(PurchaseOrderLineDto::from).toList(),
        order.getCreatedDate(),
        order.getCreatedBy(),
        order.getUpdatedDate(),
        order.getUpdatedBy(),
        order.getVersion(),
        order.isEnabled());
  }
}
