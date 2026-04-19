package com.vetsoftware.app.company.domain;

public record CompanyId(Long value) {
    public static CompanyId of(Long value) {
        return new CompanyId(value);
    }
}
