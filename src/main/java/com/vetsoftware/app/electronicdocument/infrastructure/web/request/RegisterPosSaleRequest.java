package com.vetsoftware.app.electronicdocument.infrastructure.web.request;

import com.vetsoftware.app.electronicdocument.application.command.SaleLineKind;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocumentType;
import com.vetsoftware.app.electronicdocument.domain.PaymentMeans;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.util.List;

/**
 * Venta de POS a registrar. El front nunca manda companyId (lo deriva el
 * backend del JWT). El unitPrice de cada linea es el precio final (post-promo)
 * con IVA incluido; la clasificacion tributaria la pone el servidor desde el
 * catalogo. customerOwnerId null o finalConsumer=true => consumidor final
 * anonimo.
 */
public record RegisterPosSaleRequest(
        @NotNull(message = "Debes seleccionar el tipo de documento electrónico.") ElectronicDocumentType documentType,
        boolean finalConsumer, Long customerOwnerId,
        @NotEmpty(message = "Debes asignar al menos un ítem a la venta.") @Valid List<SaleLineRequest> lines,
        @NotEmpty(message = "Debes asignar al menos un pago a la venta.") @Valid List<SalePaymentRequest> payments,
        /**
         * Idempotencia de la venta: UUID que el front genera por apertura del cobro
         * (opcional).
         */
        String clientRequestId, /**
                                 * Sede emisora (opcional). Si no viene, se usa la sede "Principal"
                                 * de la empresa.
                                 */
        Long branchId) {
    public record SaleLineRequest(
            @NotNull(message = "Debes seleccionar el tipo de ítem.") SaleLineKind kind, Long refId,
            String description,
            @NotNull(message = "La cantidad es obligatoria.") @Positive(message = "La cantidad debe ser mayor que cero.") BigDecimal quantity,
            @NotNull(message = "El precio unitario es obligatorio.") @PositiveOrZero(message = "El precio unitario no puede ser negativo.") BigDecimal unitPrice) {
    }

    public record SalePaymentRequest(
            @NotNull(message = "Debes seleccionar el medio de pago.") PaymentMeans means,
            @NotNull(message = "El monto es obligatorio.") @PositiveOrZero(message = "El monto no puede ser negativo.") BigDecimal amount) {
    }
}
