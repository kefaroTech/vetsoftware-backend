package com.vetsoftware.app.product.domain;

import java.math.BigDecimal;

public record TaxRef(Long id, String name, BigDecimal percentage) {
    public TaxRef {
        if (id == null)
            throw new IllegalArgumentException("tax id is required");
        if (name == null || name.isBlank())
            throw new IllegalArgumentException("tax name is required");
        if (percentage == null)
            throw new IllegalArgumentException("tax percentage is required");
        if (percentage.signum() < 0)
            throw new IllegalArgumentException("tax percentage cannot be negative");
    }
}
