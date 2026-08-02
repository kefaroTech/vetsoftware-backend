package com.vetsoftware.app.electronicdocument.domain;

/**
 * Régimen de IVA del adquiriente congelado en el documento. Companion propio de la feature
 * (vertical slicing): no acopla al enum de {@code owner}. El adaptador del proveedor (MATIAS) lo
 * mapea al {@code tax_regime_id} de la DIAN.
 */
public enum TaxRegime {
  RESPONSABLE_IVA,
  NO_RESPONSABLE_IVA;

  /**
   * Parsea el nombre del enum (se persiste como String en {@code customer_tax_regime}) de forma
   * tolerante: {@code null}/blank/desconocido → {@code null} (documentos antiguos sin el dato).
   */
  public static TaxRegime fromName(String name) {
    if (name == null || name.isBlank()) return null;
    try {
      return valueOf(name.trim());
    } catch (IllegalArgumentException e) {
      return null;
    }
  }
}
