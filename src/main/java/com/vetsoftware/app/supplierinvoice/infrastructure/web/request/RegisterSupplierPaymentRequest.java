package com.vetsoftware.app.supplierinvoice.infrastructure.web.request;

import com.vetsoftware.app.supplierinvoice.domain.SupplierInvoicePaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record RegisterSupplierPaymentRequest(
        @NotNull(message = "El monto es obligatorio.") @DecimalMin(value = "0.0", inclusive = false, message = "El monto debe ser mayor que cero.") BigDecimal amount,
        @NotNull(message = "La fecha del pago es obligatoria.") LocalDate paymentDate,
        @NotNull(message = "Debes seleccionar el método de pago.") SupplierInvoicePaymentMethod method,
        @Size(max = 80, message = "La referencia no puede superar los 80 caracteres.") String reference,
        @Size(max = 300, message = "La nota no puede superar los 300 caracteres.") String note,
        @NotNull(message = "No se pudo identificar la versión de la factura. Vuelve a cargarla e inténtalo de nuevo.") Long version) {
}
