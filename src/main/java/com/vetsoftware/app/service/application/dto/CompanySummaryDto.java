package com.vetsoftware.app.service.application.dto;

import com.vetsoftware.app.service.domain.CompanyRef;

public record CompanySummaryDto(Long id, String name, String identifier) {
    public static CompanySummaryDto from(CompanyRef ref) {
        return new CompanySummaryDto(ref.id(), ref.name(), ref.identifier());
    }
}
