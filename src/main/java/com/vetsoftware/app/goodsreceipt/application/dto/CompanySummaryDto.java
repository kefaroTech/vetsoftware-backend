package com.vetsoftware.app.goodsreceipt.application.dto;

import com.vetsoftware.app.goodsreceipt.domain.CompanyRef;

public record CompanySummaryDto(Long id, String name, String identifier) {
    public static CompanySummaryDto from(CompanyRef company) {
        return new CompanySummaryDto(company.id(), company.name(), company.identifier());
    }
}
