package com.vetsoftware.app.inventory.infrastructure.persistence;

import com.vetsoftware.app.companysettings.infrastructure.persistence.CompanySettingJpaRepository;
import com.vetsoftware.app.inventory.application.port.out.NegativeStockPolicyPort;
import org.springframework.stereotype.Component;

/**
 * Política de stock negativo respaldada por {@code company_settings} (por empresa). El flag {@code
 * inventory.allow_negative_stock} se togglea por el admin de cada empresa (ver
 * CompanySettingController); ausencia de la fila = {@code false} (bloquear salida sin existencias).
 */
@Component
public class CompanySettingsNegativeStockPolicy implements NegativeStockPolicyPort {

  private static final String KEY = "inventory.allow_negative_stock";

  private final CompanySettingJpaRepository companySettingJpaRepository;

  public CompanySettingsNegativeStockPolicy(
      CompanySettingJpaRepository companySettingJpaRepository) {
    this.companySettingJpaRepository = companySettingJpaRepository;
  }

  @Override
  public boolean allowsNegative(Long companyId) {
    if (companyId == null) return false;
    return companySettingJpaRepository
        .findByCompanyIdAndPropertyName(companyId, KEY)
        .map(e -> "true".equalsIgnoreCase(e.getValue()))
        .orElse(false);
  }
}
