package com.vetsoftware.app.service.infrastructure.web.response;

import com.vetsoftware.app.service.domain.TaxTreatment;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ServiceResponse(Long id, String name, BigDecimal price, TaxTreatment taxTreatment,
        String notes, ServiceCategorySummary serviceCategory, TaxSummary tax,
        CompanySummary company, LocalDateTime createdDate, LocalDateTime updatedDate,
        Long updatedBy, Long version, boolean enabled) {
}
