package com.vetsoftware.app.electronicdocument.infrastructure.web.request;

import com.vetsoftware.app.electronicdocument.domain.DebitNoteReason;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

/**
 * {@code additionalAmount} opcional: null ⇒ clona el original (heredado); un
 * monto (&gt; 0) ⇒ incremento real.
 */
public record IssueDebitNoteRequest(
        @NotNull(message = "Debes seleccionar el motivo de la nota débito.") DebitNoteReason reason,
        @Positive(message = "El monto adicional debe ser mayor que cero.") BigDecimal additionalAmount) {
}
