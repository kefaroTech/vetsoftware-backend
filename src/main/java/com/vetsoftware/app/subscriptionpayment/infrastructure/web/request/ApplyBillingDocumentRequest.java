package com.vetsoftware.app.subscriptionpayment.infrastructure.web.request;

import com.vetsoftware.app.subscriptionpayment.domain.ApplicationSourceKind;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Los cuatro campos de referencia -{@code paymentId}, {@code sourceDocumentId},
 * {@code withholdingId} y {@code creditEntryId}- son
 * <strong>excluyentes</strong>: uno u otro segun {@code sourceKind}, nunca dos,
 * y ninguno en {@code ROUNDING} y {@code WRITE_OFF}. No se declara con
 * anotaciones de Bean Validation porque es una invariante de dominio -la valida
 * la entidad y la vuelve a validar {@code chk_bda_source_exclusive}-, y
 * duplicarla aqui crearia dos verdades sobre lo mismo.
 *
 * <p>
 * <strong>El autorizante del castigo NO viaja en el cuerpo, y esa ausencia es
 * deliberada.</strong> Lo pone el controller desde el principal
 * ({@code authz.currentSystemUserId()}), igual que el registro de la factura
 * externa. Si llegara en el JSON, quien castiga una deuda elegiria a quien
 * atribuirle la firma, que es lo contrario de firmar.
 */
public record ApplyBillingDocumentRequest(
        @NotNull(message = "Debes indicar la factura que se salda.") Long targetDocumentId,
        @NotNull(message = "Debes indicar el origen del abono.") ApplicationSourceKind sourceKind,
        Long paymentId, Long sourceDocumentId,
        /** La retencion practicada, cuando el origen es {@code WITHHOLDING}. */
        Long withholdingId,
        /** El lote de saldo a favor, cuando el origen es {@code CUSTOMER_CREDIT}. */
        Long creditEntryId,
        @NotNull(message = "El valor aplicado es obligatorio.") @Positive(message = "El valor aplicado debe ser mayor que cero.") BigDecimal appliedAmount,
        /**
         * Motivo del castigo. Obligatorio cuando el origen es {@code WRITE_OFF} y
         * prohibido en el resto; lo comprueba el dominio, que es donde vive la regla.
         */
        @Size(max = 255, message = "El motivo del castigo no puede superar los 255 caracteres.") String writeOffReason,
        /**
         * Cuando el asiento cuenta. Opcional: si no llega se usa el dia de la
         * aplicacion, que es lo correcto en un pago y en una nota credito. En una
         * retencion no: practicada el 30 de octubre y registrada el 3 de noviembre
         * pertenece a octubre.
         */
        LocalDate valueDate,
        /**
         * Llave de idempotencia: con ella, el doble clic devuelve la aplicacion que ya
         * se creo en vez de saldar la factura dos veces.
         */
        @Size(max = 64, message = "El identificador de la solicitud no puede superar los 64 caracteres.") String clientRequestId) {
}
