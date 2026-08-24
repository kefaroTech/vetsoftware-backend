package com.vetsoftware.app.subscriptionpayment.infrastructure.web.request;

import com.vetsoftware.app.subscriptionpayment.domain.ApplicationSourceKind;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * {@code paymentId} y {@code sourceDocumentId} son
 * <strong>excluyentes</strong>: uno u otro segun {@code sourceKind}, nunca los
 * dos. No se declara con anotaciones de Bean Validation porque es una
 * invariante de dominio -la valida la entidad y la vuelve a validar
 * {@code chk_bda_source_exclusive}-, y duplicarla aqui crearia dos verdades
 * sobre lo mismo.
 */
public record ApplyBillingDocumentRequest(
        @NotNull(message = "Debes indicar la factura que se salda.") Long targetDocumentId,
        @NotNull(message = "Debes indicar el origen: pago o nota credito.") ApplicationSourceKind sourceKind,
        Long paymentId, Long sourceDocumentId,
        @NotNull(message = "El valor aplicado es obligatorio.") @Positive(message = "El valor aplicado debe ser mayor que cero.") BigDecimal appliedAmount,
        /**
         * Llave de idempotencia: con ella, el doble clic devuelve la aplicacion que ya
         * se creo en vez de saldar la factura dos veces.
         */
        @Size(max = 64, message = "El identificador de la solicitud no puede superar los 64 caracteres.") String clientRequestId) {
}
