package com.vetsoftware.app.purchaseorder.infrastructure.web.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record PurchaseOrderLineRequest(
        @NotNull(message = "Debes seleccionar el producto.") Long productId,
        @Positive(message = "La cantidad solicitada debe ser mayor que cero.") int quantityOrdered,
        @NotNull(message = "El costo unitario es obligatorio.") @DecimalMin(value = "0.0", message = "El costo unitario no puede ser negativo.") BigDecimal unitCost) {
}
