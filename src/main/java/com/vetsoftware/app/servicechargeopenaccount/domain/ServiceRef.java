package com.vetsoftware.app.servicechargeopenaccount.domain;

import java.math.BigDecimal;

public record ServiceRef(Long id, String name, BigDecimal price) {
    public ServiceRef {
        if (id == null) throw new IllegalArgumentException("service id is required");
        if (name == null || name.isBlank()) throw new IllegalArgumentException("service name is required");
        if (price == null) throw new IllegalArgumentException("service price is required");
    }
}
