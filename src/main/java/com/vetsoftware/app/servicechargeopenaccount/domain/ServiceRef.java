package com.vetsoftware.app.servicechargeopenaccount.domain;

import java.math.BigDecimal;

/**
 * Companion VO del servicio del catálogo. Incluye el impuesto del catálogo ({@code hasTax} + {@code tax})
 * para que el cargo congele el desglose tributario al crearse. El precio ({@code price}) se interpreta
 * CON IVA incluido.
 */
public record ServiceRef(Long id, String name, BigDecimal price, boolean hasTax, TaxRef tax,
                         String taxTreatment) {
    public ServiceRef {
        if (id == null) throw new IllegalArgumentException("service id is required");
        if (name == null || name.isBlank()) throw new IllegalArgumentException("service name is required");
        if (price == null) throw new IllegalArgumentException("service price is required");
    }

    /** Compat: servicio sin información de impuesto (lectura / casos sin catálogo de impuesto). */
    public ServiceRef(Long id, String name, BigDecimal price) {
        this(id, name, price, false, null, null);
    }
}
