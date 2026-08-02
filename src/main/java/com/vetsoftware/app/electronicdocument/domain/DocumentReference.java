package com.vetsoftware.app.electronicdocument.domain;

import java.time.LocalDate;

/**
 * Referencia al documento corregido por una nota credito/debito (UBL BillingReference). Apunta al
 * CUFE, numero (prefijo+consecutivo) y fecha de emision de la factura original VALIDADA. Es una
 * copia congelada: si la factura cambia de estado, la nota conserva esta referencia.
 */
public record DocumentReference(String cufe, String prefix, Long number, LocalDate issueDate) {
  public DocumentReference {
    if (cufe == null || cufe.isBlank())
      throw new IllegalArgumentException("referenced cufe is required");
    if (number == null) throw new IllegalArgumentException("referenced number is required");
    if (issueDate == null) throw new IllegalArgumentException("referenced issueDate is required");
  }
}
