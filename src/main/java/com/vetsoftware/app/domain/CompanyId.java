package com.vetsoftware.app.domain;

public record CompanyId(Long value) {
    public static CompanyId of(Long value) {
        return new CompanyId(value);
    }
}
