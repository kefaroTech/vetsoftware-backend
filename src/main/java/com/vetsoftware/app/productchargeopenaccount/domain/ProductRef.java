package com.vetsoftware.app.productchargeopenaccount.domain;

import java.math.BigDecimal;

/**
 * Companion VO del producto del catálogo. Incluye el impuesto del catálogo ({@code hasTax} + {@code tax})
 * para que el cargo congele el desglose tributario al crearse. El precio ({@code salePrice}) se interpreta
 * CON IVA incluido.
 */
public record ProductRef(Long id, String name, String code, BigDecimal salePrice, boolean hasTax, TaxRef tax) {
    public ProductRef {
        if (id == null) throw new IllegalArgumentException("product id is required");
    }

    /** Compat: producto sin información de impuesto (lectura / casos sin catálogo de impuesto). */
    public ProductRef(Long id, String name, String code, BigDecimal salePrice) {
        this(id, name, code, salePrice, false, null);
    }
}
