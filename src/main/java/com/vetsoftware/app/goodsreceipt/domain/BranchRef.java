package com.vetsoftware.app.goodsreceipt.domain;

/**
 * Companion VO: sede (feature {@code branch}) donde entra la mercancía
 * recibida.
 */
public record BranchRef(Long id, String name) {
    public BranchRef {
        if (id == null)
            throw new IllegalArgumentException("branch id is required");
        if (name == null || name.isBlank())
            throw new IllegalArgumentException("branch name is required");
    }
}
