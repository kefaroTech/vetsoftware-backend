package com.vetsoftware.app.productcategory.infrastructure.web.response;

import java.time.LocalDateTime;

public record ProductCategoryResponse(
        Long id,
        String name,
        String description,
        CompanySummary company,
        LocalDateTime createdDate,
        LocalDateTime updatedDate,
        Long updatedBy,
        boolean enabled
) {}
