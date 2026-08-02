package com.vetsoftware.app.tax.application.port.out;

public interface TaxChildrenQueryPort {
  boolean existsActiveByTaxId(Long taxId);
}
