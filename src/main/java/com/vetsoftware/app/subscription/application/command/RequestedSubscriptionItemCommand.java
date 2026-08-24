package com.vetsoftware.app.subscription.application.command;

import java.time.LocalDate;

/** Selección comercial sin ningún snapshot aportado por el cliente. */
public record RequestedSubscriptionItemCommand(Long catalogItemId, Integer quantity,
        LocalDate effectiveFrom, LocalDate effectiveTo) {
}
