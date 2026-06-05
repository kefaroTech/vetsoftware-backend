package com.vetsoftware.app.productchargeopenaccount.domain;

import java.math.BigDecimal;

public record ProductRef(Long id, String name, String code, BigDecimal salePrice) {
    public ProductRef {
        if (id == null) throw new IllegalArgumentException("product id is required");
    }
}
