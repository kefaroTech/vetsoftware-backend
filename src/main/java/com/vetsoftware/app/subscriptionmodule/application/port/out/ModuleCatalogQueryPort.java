package com.vetsoftware.app.subscriptionmodule.application.port.out;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Catálogo de plataforma: {@code catalog_items}, {@code catalog_prices} y
 * {@code catalog_item_limits}. Sin {@code company_id}: es global.
 */
public interface ModuleCatalogQueryPort {

    List<ModuleCatalogEntry> listActiveModules(LocalDate today);

    List<ModuleCeilingDefinition> findCeilings(Long catalogItemId);

    record ModuleCatalogEntry(Long catalogItemId, String code, String name, String shortDescription,
            String trialEligibility, boolean selfService, BigDecimal monthlyPrice,
            BigDecimal annualPrice) {
    }

    record ModuleCeilingDefinition(String dimensionCode, String measureKind, int limitQuantity,
            int warnThreshold, String enforcement) {
    }
}
