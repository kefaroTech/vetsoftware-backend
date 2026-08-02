package com.vetsoftware.app.membershipsubmodule.domain;

public record MembershipRef(Long id, String name) {
    public MembershipRef {
        if (id == null)
            throw new IllegalArgumentException("membership id is required");
        if (name == null || name.isBlank())
            throw new IllegalArgumentException("membership name is required");
    }
}
