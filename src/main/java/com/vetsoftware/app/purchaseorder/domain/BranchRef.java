package com.vetsoftware.app.purchaseorder.domain;

/**
 * Companion VO: sede (feature {@code branch}) a la que se destina la orden de
 * compra.
 */
public record BranchRef(Long id, String name) {
    public BranchRef {
        if (id == null)
            throw new IllegalArgumentException("branch id is required");
        if (name == null || name.isBlank())
            throw new IllegalArgumentException("branch name is required");
    }
}
