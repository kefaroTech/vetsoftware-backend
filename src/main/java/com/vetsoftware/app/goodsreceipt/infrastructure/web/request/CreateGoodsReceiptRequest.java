package com.vetsoftware.app.goodsreceipt.infrastructure.web.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

public record CreateGoodsReceiptRequest(
        @NotNull(message = "Debes seleccionar la sede.") Long branchId,
        @NotNull(message = "Debes seleccionar el proveedor.") Long supplierId, Long purchaseOrderId,
        @NotNull(message = "La fecha de recepción es obligatoria.") LocalDate receiptDate,
        @Size(max = 60, message = "El número de factura del proveedor no puede superar los 60 caracteres.") String supplierInvoiceNumber,
        @Size(max = 500, message = "Las notas no pueden superar los 500 caracteres.") String notes,
        @NotEmpty(message = "Debes asignar al menos un producto a la recepción.") @Valid List<GoodsReceiptLineRequest> lines) {
}
