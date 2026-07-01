package com.vetsoftware.app.servicecategory.infrastructure.web.response;

import java.time.LocalDateTime;

public record ServiceCategoryResponse(
        Long id,
        String name,
        String description,
        CompanySummary company,
        LocalDateTime createdDate,
        LocalDateTime updatedDate,
        Long updatedBy,
        Long version,
        boolean enabled
) {}
