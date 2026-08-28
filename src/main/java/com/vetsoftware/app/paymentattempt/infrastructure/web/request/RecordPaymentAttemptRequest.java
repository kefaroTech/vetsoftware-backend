package com.vetsoftware.app.paymentattempt.infrastructure.web.request;

import com.vetsoftware.app.paymentattempt.domain.DeclineKind;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * <strong>Sin {@code companyId}: la regla no admite la excepcion que parecia
 * admitir.</strong> Esta ruta vive bajo {@code /system/**} y su puerto esta
 * cerrado a {@code hasRole('SYSTEM')} a secas, asi que el riesgo de
 * suplantacion no aplica; pero la regla dura
 * {@code EMPRESA_NO_VIAJA_EN_EL_CUERPO} mira <em>todo</em> {@code @RequestBody}
 * sin mirar la ruta ni el rol. La empresa viaja como {@code @RequestParam}, que
 * es la forma permitida.
 *
 * <p>
 * <strong>Sin {@code attemptNumber}</strong>: el consecutivo lo calcula el caso
 * de uso dentro de la transaccion. Dejarselo elegir a quien llama es como se
 * cuelan huecos y colisiones contra {@code uq_payment_attempts_number}.
 *
 * @param nextAttemptAt
 *            vacio obligatoriamente si {@code declineKind} es {@code HARD}: no
 *            hay siguiente hasta que aparezca otra tarjeta
 */
public record RecordPaymentAttemptRequest(
        @NotNull(message = "Debes indicar el documento de cobro.") Long billingDocumentId,
        Long paymentMethodId,
        @NotNull(message = "Debes indicar la pasarela.") @Size(max = 40, message = "La pasarela no puede superar los 40 caracteres.") String gateway,
        @NotNull(message = "El valor intentado es obligatorio.") @Positive(message = "El valor intentado debe ser mayor que cero.") BigDecimal requestedAmount,
        @Size(max = 50, message = "El codigo de rechazo no puede superar los 50 caracteres.") String gatewayDeclineCode,
        @NotNull(message = "Debes indicar la clase del rechazo.") DeclineKind declineKind,
        @NotNull(message = "Debes indicar cuando se intento el cobro.") LocalDateTime attemptedAt,
        LocalDateTime nextAttemptAt) {
}
