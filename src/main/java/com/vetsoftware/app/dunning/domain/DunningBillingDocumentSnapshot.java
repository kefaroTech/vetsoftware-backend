package com.vetsoftware.app.dunning.domain;

import java.time.LocalDate;

/** Datos mínimos de una factura externa para decidir la mora. */
public record DunningBillingDocumentSnapshot(BillingDocumentRef document, Long subscriptionId,
        LocalDate dueDate) {
    public DunningBillingDocumentSnapshot {
        if (document == null)
            throw new IllegalArgumentException("document is required");
        if (subscriptionId == null)
            throw new IllegalArgumentException("subscriptionId is required");
        if (dueDate == null)
            throw new IllegalArgumentException("dueDate is required");
    }
}
