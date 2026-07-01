package com.vetsoftware.app.tax.application.dto;

import com.vetsoftware.app.tax.domain.Tax;
import com.vetsoftware.app.tax.domain.TaxScheme;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TaxDto(
        Long id,
        String name,
        BigDecimal percentage,
        TaxScheme taxScheme,
        CompanySummaryDto company,
        LocalDateTime createdDate,
        LocalDateTime updatedDate,
        Long updatedBy,
        boolean enabled
) {
    public static TaxDto from(Tax tax) {
        return new TaxDto(
                tax.getId(),
                tax.getName(),
                tax.getPercentage(),
                tax.getTaxScheme(),
                tax.getCompany() == null ? null : CompanySummaryDto.from(tax.getCompany()),
                tax.getCreatedDate(),
                tax.getUpdatedDate(),
                tax.getUpdatedBy(),
                tax.isEnabled());
    }
}
