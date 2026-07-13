package com.vetsoftware.app.supplierinvoice.domain;

/** Companion VO: proveedor (feature {@code supplier}) que emite la factura. {@code taxId} alimenta el libro de compras. */
public record SupplierRef(Long id, String name, String taxId) {
    public SupplierRef {
        if (id == null) throw new IllegalArgumentException("supplier id is required");
        if (name == null || name.isBlank()) throw new IllegalArgumentException("supplier name is required");
    }
}
