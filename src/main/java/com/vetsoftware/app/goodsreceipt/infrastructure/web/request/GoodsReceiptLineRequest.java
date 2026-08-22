package com.vetsoftware.app.goodsreceipt.infrastructure.web.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record GoodsReceiptLineRequest(
        @NotNull(message = "Debes seleccionar el producto.") Long productId,
        Long purchaseOrderLineId,
        @Size(max = 60, message = "El número de lote no puede superar los 60 caracteres.") String lotNumber,
        LocalDate expireDate,
        @Positive(message = "La cantidad recibida debe ser mayor que cero.") int quantityReceived,
        @NotNull(message = "El costo unitario es obligatorio.") @DecimalMin(value = "0.0", message = "El costo unitario no puede ser negativo.") BigDecimal unitCost) {
}
