package com.vetsoftware.app.productchargeopenaccount.domain;

public record OpenAccountRef(Long id, Long companyId) {
    public OpenAccountRef {
        if (id == null) throw new IllegalArgumentException("openAccount id is required");
    }
}
