package com.vetsoftware.app.supplierinvoice.domain;

/**
 * Companion VO: sede (feature {@code branch}) a la que se imputa la factura de
 * proveedor.
 */
public record BranchRef(Long id, String name) {
    public BranchRef {
        if (id == null)
            throw new IllegalArgumentException("branch id is required");
        if (name == null || name.isBlank())
            throw new IllegalArgumentException("branch name is required");
    }
}
