package com.vetsoftware.app.openaccount.domain;

public record BranchRef(Long id, String name, String code) {
    public BranchRef {
        if (id == null) throw new IllegalArgumentException("branch id is required");
        if (name == null || name.isBlank()) throw new IllegalArgumentException("branch name is required");
        if (code == null || code.isBlank()) throw new IllegalArgumentException("branch code is required");
    }
}
