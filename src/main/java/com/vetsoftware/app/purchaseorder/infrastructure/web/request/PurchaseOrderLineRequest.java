package com.vetsoftware.app.purchaseorder.infrastructure.web.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record PurchaseOrderLineRequest(
        @NotNull Long productId,
        @Positive int quantityOrdered,
        @NotNull @DecimalMin("0.0") BigDecimal unitCost
) {}
