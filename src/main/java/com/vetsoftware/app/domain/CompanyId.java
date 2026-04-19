package com.vetsoftware.app.domain;

import java.util.UUID;

public record CompanyId(String value) {
    public static CompanyId generate() {
        return new CompanyId(UUID.randomUUID().toString());
    }

    public static CompanyId of(String value) {
        return new CompanyId(value);
    }
}
