package com.vetsoftware.app.subscriptionmodule.application.port.out;

import java.time.LocalDate;
import java.util.Optional;

/**
 * La línea vigente de {@code subscription_items} de un artículo del catálogo
 * para una empresa, si la tiene contratada (en cualquier {@code charge_mode}).
 */
public interface CompanyModuleLineQueryPort {

    Optional<CompanyModuleLine> findCurrentLine(Long companyId, Long catalogItemId);

    record CompanyModuleLine(String chargeMode, LocalDate trialEndDate) {
    }
}
