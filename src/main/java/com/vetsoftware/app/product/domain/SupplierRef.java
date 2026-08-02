package com.vetsoftware.app.product.domain;

/**
 * Companion VO: proveedor (feature {@code supplier}) referenciado por un producto. Reemplaza al
 * antiguo texto libre {@code provider} — que se conserva como fallback durante la migración. La FK
 * es OPCIONAL: un producto puede no tener proveedor asociado, por eso {@code Product.supplier}
 * puede ser null; pero si está presente, este VO exige sus invariantes.
 */
public record SupplierRef(Long id, String name) {
  public SupplierRef {
    if (id == null) throw new IllegalArgumentException("supplier id is required");
    if (name == null || name.isBlank())
      throw new IllegalArgumentException("supplier name is required");
  }
}
