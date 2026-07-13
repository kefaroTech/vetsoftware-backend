package com.vetsoftware.app.supplier.application.dto;

import com.vetsoftware.app.supplier.domain.CompanyRef;

public record CompanySummaryDto(Long id, String name, String identifier) {
    public static CompanySummaryDto from(CompanyRef company) {
        return new CompanySummaryDto(company.id(), company.name(), company.identifier());
    }
}
