package com.vetsoftware.app.employee.domain;

public record RoleSnapshot(Long id, String name, String code) {
    public RoleSnapshot {
        if (id == null) throw new IllegalArgumentException("role id is required");
        if (name == null || name.isBlank()) throw new IllegalArgumentException("role name is required");
        if (code == null || code.isBlank()) throw new IllegalArgumentException("role code is required");
    }
}
