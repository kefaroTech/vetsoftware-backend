package com.vetsoftware.app.company.domain;

public record MembershipRef(Long id, String name, String status) {
    public MembershipRef {
        if (id == null) throw new IllegalArgumentException("membership id is required");
        if (name == null || name.isBlank()) throw new IllegalArgumentException("membership name is required");
        if (status == null || status.isBlank()) throw new IllegalArgumentException("membership status is required");
    }
}
