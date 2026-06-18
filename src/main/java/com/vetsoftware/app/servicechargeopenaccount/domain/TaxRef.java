package com.vetsoftware.app.servicechargeopenaccount.domain;

import java.math.BigDecimal;

public record TaxRef(Long id, String name, BigDecimal percentage, String scheme) {
    public TaxRef {
        if (id == null) throw new IllegalArgumentException("tax id is required");
        if (name == null || name.isBlank()) throw new IllegalArgumentException("tax name is required");
        if (percentage == null) throw new IllegalArgumentException("tax percentage is required");
    }

    /** Compat: impuesto sin esquema (lecturas o datos previos a congelar IVA vs INC). */
    public TaxRef(Long id, String name, BigDecimal percentage) {
        this(id, name, percentage, null);
    }
}
