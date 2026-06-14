package com.vetsoftware.app.electronicdocument.infrastructure.web.request;

import com.vetsoftware.app.electronicdocument.domain.DebitNoteReason;
import jakarta.validation.constraints.NotNull;

public record IssueDebitNoteRequest(@NotNull DebitNoteReason reason) {}
