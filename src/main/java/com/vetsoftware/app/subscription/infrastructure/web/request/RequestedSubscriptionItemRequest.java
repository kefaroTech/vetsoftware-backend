package com.vetsoftware.app.subscription.infrastructure.web.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/** Selección de catálogo; los snapshots se resuelven después en servidor. */
public record RequestedSubscriptionItemRequest(@NotNull Long catalogItemId,
        @NotNull @Min(1) Integer quantity, LocalDate effectiveFrom, LocalDate effectiveTo) {
}
