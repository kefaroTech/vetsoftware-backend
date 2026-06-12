package com.vetsoftware.app.service.application.command;

import com.vetsoftware.app.service.domain.TaxTreatment;
import java.math.BigDecimal;

public record UpdateServiceCommand(
        Long id,
        String name,
        BigDecimal price,
        TaxTreatment taxTreatment,
        String notes,
        Long serviceCategoryId,
        Long taxId,
        Long companyId
) {}
