package com.vetsoftware.app.supplierinvoice.infrastructure.web.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record UpdateSupplierInvoiceRequest(Long branchId,
        @NotNull(message = "Debes seleccionar el proveedor.") Long supplierId, Long purchaseOrderId,
        Long goodsReceiptId,
        @NotBlank(message = "El número de la factura es obligatorio.") @Size(max = 60, message = "El número de la factura no puede superar los 60 caracteres.") String invoiceNumber,
        @NotNull(message = "La fecha de emisión es obligatoria.") LocalDate issueDate,
        @NotNull(message = "La fecha de vencimiento es obligatoria.") LocalDate dueDate,
        @NotNull(message = "El subtotal es obligatorio.") @DecimalMin(value = "0.0", message = "El subtotal no puede ser negativo.") BigDecimal subtotal,
        @NotNull(message = "El valor de los impuestos es obligatorio.") @DecimalMin(value = "0.0", message = "El valor de los impuestos no puede ser negativo.") BigDecimal taxAmount,
        @DecimalMin(value = "0.0", message = "El valor de las retenciones no puede ser negativo.") BigDecimal withholdingAmount,
        @Size(max = 500, message = "Las notas no pueden superar los 500 caracteres.") String notes,
        @NotNull(message = "No se pudo identificar la versión de la factura. Vuelve a cargarla e inténtalo de nuevo.") Long version) {
}
