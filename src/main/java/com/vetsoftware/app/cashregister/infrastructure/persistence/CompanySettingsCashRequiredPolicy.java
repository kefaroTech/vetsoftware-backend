package com.vetsoftware.app.cashregister.infrastructure.persistence;

import com.vetsoftware.app.cashregister.application.port.out.CashRequiredPolicyPort;
import com.vetsoftware.app.companysettings.infrastructure.persistence.CompanySettingJpaRepository;
import org.springframework.stereotype.Component;

/**
 * "¿Se exige caja para cobrar?" respaldado por {@code company_settings} (por empresa). El flag
 * {@code cashregister.required} lo togglea el admin de cada empresa; ausencia de la fila = {@code true} (bloquear el
 * cobro sin caja abierta). Mismo patrón que {@code CompanySettingsNegativeStockPolicy} del inventario.
 */
@Component
public class CompanySettingsCashRequiredPolicy implements CashRequiredPolicyPort {

    private static final String KEY = "cashregister.required";

    private final CompanySettingJpaRepository companySettingJpaRepository;

    public CompanySettingsCashRequiredPolicy(CompanySettingJpaRepository companySettingJpaRepository) {
        this.companySettingJpaRepository = companySettingJpaRepository;
    }

    @Override
    public boolean isCashRequired(Long companyId) {
        if (companyId == null) return true;
        return companySettingJpaRepository.findByCompanyIdAndPropertyName(companyId, KEY)
            .map(e -> !"false".equalsIgnoreCase(e.getValue()))
            .orElse(true);
    }
}
