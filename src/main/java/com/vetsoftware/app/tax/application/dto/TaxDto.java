package com.vetsoftware.app.tax.application.dto;

import com.vetsoftware.app.tax.domain.Tax;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TaxDto(
        Long id,
        String name,
        BigDecimal percentage,
        CompanySummaryDto company,
        LocalDateTime createdDate,
        boolean enabled
) {
    public static TaxDto from(Tax tax) {
        return new TaxDto(
                tax.getId(),
                tax.getName(),
                tax.getPercentage(),
                tax.getCompany() == null ? null : CompanySummaryDto.from(tax.getCompany()),
                tax.getCreatedDate(),
                tax.isEnabled());
    }
}
