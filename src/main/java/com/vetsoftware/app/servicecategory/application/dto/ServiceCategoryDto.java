package com.vetsoftware.app.servicecategory.application.dto;

import com.vetsoftware.app.servicecategory.domain.ServiceCategory;
import java.time.LocalDateTime;

public record ServiceCategoryDto(
        Long id,
        String name,
        String description,
        CompanySummaryDto company,
        LocalDateTime createdDate,
        boolean enabled
) {
    public static ServiceCategoryDto from(ServiceCategory serviceCategory) {
        return new ServiceCategoryDto(
                serviceCategory.getId(),
                serviceCategory.getName(),
                serviceCategory.getDescription(),
                serviceCategory.getCompany() == null ? null : CompanySummaryDto.from(serviceCategory.getCompany()),
                serviceCategory.getCreatedDate(),
                serviceCategory.isEnabled());
    }
}
