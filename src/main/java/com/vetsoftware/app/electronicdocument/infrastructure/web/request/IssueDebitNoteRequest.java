package com.vetsoftware.app.electronicdocument.infrastructure.web.request;

import com.vetsoftware.app.electronicdocument.domain.DebitNoteReason;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

/**
 * {@code additionalAmount} opcional: null ⇒ clona el original (heredado); un monto (&gt; 0) ⇒
 * incremento real.
 */
public record IssueDebitNoteRequest(
    @NotNull DebitNoteReason reason, @Positive BigDecimal additionalAmount) {}
