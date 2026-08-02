package com.vetsoftware.app.purchaseorder.application.dto;

import com.vetsoftware.app.purchaseorder.domain.PurchaseOrderLine;
import java.math.BigDecimal;

public record PurchaseOrderLineDto(Long id, ProductSummaryDto product, int quantityOrdered,
        BigDecimal unitCost, int quantityReceived, int pendingQuantity, boolean fullyReceived) {
    public static PurchaseOrderLineDto from(PurchaseOrderLine line) {
        return new PurchaseOrderLineDto(line.getId(), ProductSummaryDto.from(line.getProduct()),
                line.getQuantityOrdered(), line.getUnitCost(), line.getQuantityReceived(),
                line.pendingQuantity(), line.isFullyReceived());
    }
}
