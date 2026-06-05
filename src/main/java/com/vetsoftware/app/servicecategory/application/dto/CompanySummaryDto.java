package com.vetsoftware.app.servicecategory.application.dto;

import com.vetsoftware.app.servicecategory.domain.CompanyRef;

public record CompanySummaryDto(Long id, String name, String identifier) {
    public static CompanySummaryDto from(CompanyRef ref) {
        return new CompanySummaryDto(ref.id(), ref.name(), ref.identifier());
    }
}
