package com.vetsoftware.app.purchaseorder.infrastructure.web.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

public record UpdatePurchaseOrderRequest(
        @NotNull(message = "Debes seleccionar la sede.") Long branchId,
        @NotNull(message = "Debes seleccionar el proveedor.") Long supplierId,
        @NotNull(message = "La fecha de la orden es obligatoria.") LocalDate orderDate,
        LocalDate expectedDate,
        @Size(max = 500, message = "Las notas no pueden superar los 500 caracteres.") String notes,
        @NotEmpty(message = "Debes asignar al menos un producto a la orden.") @Valid List<PurchaseOrderLineRequest> lines,
        @NotNull(message = "No se pudo identificar la versión de la orden de compra. Vuelve a cargarla e inténtalo de nuevo.") Long version) {
}
