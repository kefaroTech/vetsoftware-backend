package com.vetsoftware.app.electronicdocument.infrastructure.web.request;

import com.vetsoftware.app.electronicdocument.domain.CreditNoteReason;
import jakarta.validation.constraints.NotNull;

public record IssueCreditNoteRequest(@NotNull CreditNoteReason reason) {}
