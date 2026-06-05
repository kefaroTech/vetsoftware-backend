package com.vetsoftware.app.productchargeopenaccount.application.dto;

import com.vetsoftware.app.productchargeopenaccount.domain.ProductChargeOpenAccount;
import java.time.LocalDateTime;

public record ProductChargeOpenAccountDto(
        Long id,
        AnimalSummaryDto animal,
        ProductSummaryDto product,
        OpenAccountSummaryDto openAccount,
        EmployeeSummaryDto createdBy,
        LocalDateTime createdDate,
        boolean enabled
) {
    public static ProductChargeOpenAccountDto from(ProductChargeOpenAccount entity) {
        return new ProductChargeOpenAccountDto(
                entity.getId(),
                AnimalSummaryDto.from(entity.getAnimal()),
                ProductSummaryDto.from(entity.getProduct()),
                OpenAccountSummaryDto.from(entity.getOpenAccount()),
                entity.getCreatedBy() == null ? null : EmployeeSummaryDto.from(entity.getCreatedBy()),
                entity.getCreatedDate(),
                entity.isEnabled());
    }
}
