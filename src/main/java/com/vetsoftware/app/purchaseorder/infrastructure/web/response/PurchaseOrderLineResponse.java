package com.vetsoftware.app.purchaseorder.infrastructure.web.response;

import java.math.BigDecimal;

public record PurchaseOrderLineResponse(
    Long id,
    ProductSummary product,
    int quantityOrdered,
    BigDecimal unitCost,
    int quantityReceived,
    int pendingQuantity,
    boolean fullyReceived) {}
