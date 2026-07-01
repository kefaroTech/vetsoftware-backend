package com.vetsoftware.app.service.application.dto;

import com.vetsoftware.app.service.domain.Service;
import com.vetsoftware.app.service.domain.TaxTreatment;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ServiceDto(
        Long id,
        String name,
        BigDecimal price,
        TaxTreatment taxTreatment,
        String notes,
        ServiceCategorySummaryDto serviceCategory,
        TaxSummaryDto tax,
        CompanySummaryDto company,
        LocalDateTime createdDate,
        LocalDateTime updatedDate,
        Long updatedBy,
        Long version,
        boolean enabled
) {
    public static ServiceDto from(Service service) {
        return new ServiceDto(
                service.getId(),
                service.getName(),
                service.getPrice(),
                service.getTaxTreatment(),
                service.getNotes(),
                ServiceCategorySummaryDto.from(service.getServiceCategory()),
                service.getTax() == null ? null : TaxSummaryDto.from(service.getTax()),
                CompanySummaryDto.from(service.getCompany()),
                service.getCreatedDate(),
                service.getUpdatedDate(),
                service.getUpdatedBy(),
                service.getVersion(),
                service.isEnabled());
    }
}
