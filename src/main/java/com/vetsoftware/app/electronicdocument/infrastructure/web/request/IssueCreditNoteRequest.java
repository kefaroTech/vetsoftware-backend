package com.vetsoftware.app.electronicdocument.infrastructure.web.request;

import com.vetsoftware.app.electronicdocument.domain.CreditNoteReason;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

/**
 * {@code partialAmount} opcional: null ⇒ nota crédito TOTAL (anulación); un
 * monto (≤ total) ⇒ nota PARCIAL.
 */
public record IssueCreditNoteRequest(
        @NotNull(message = "Debes seleccionar el motivo de la nota crédito.") CreditNoteReason reason,
        @Positive(message = "El monto parcial debe ser mayor que cero.") BigDecimal partialAmount) {
}
