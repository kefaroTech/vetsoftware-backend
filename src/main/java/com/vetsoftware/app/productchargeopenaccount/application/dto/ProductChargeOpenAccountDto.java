package com.vetsoftware.app.productchargeopenaccount.application.dto;

import com.vetsoftware.app.productchargeopenaccount.domain.ProductChargeOpenAccount;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProductChargeOpenAccountDto(Long id, AnimalSummaryDto animal,
        ProductSummaryDto product, BigDecimal unitPrice, int quantity, boolean hasTax,
        BigDecimal taxPercentage, String taxName, BigDecimal baseAmount, BigDecimal taxAmount,
        BigDecimal totalAmount, OpenAccountSummaryDto openAccount, EmployeeSummaryDto createdBy,
        LocalDateTime createdDate, boolean enabled, boolean voided, EmployeeSummaryDto voidedBy,
        LocalDateTime voidedAt, String voidReason) {
    public static ProductChargeOpenAccountDto from(ProductChargeOpenAccount entity) {
        return new ProductChargeOpenAccountDto(entity.getId(),
                AnimalSummaryDto.from(entity.getAnimal()),
                ProductSummaryDto.from(entity.getProduct()), entity.getUnitPrice(),
                entity.getQuantity(), entity.isHasTax(), entity.getTaxPercentage(),
                entity.getTaxName(), entity.getBaseAmount(), entity.getTaxAmount(),
                entity.getTotalAmount(), OpenAccountSummaryDto.from(entity.getOpenAccount()),
                entity.getCreatedBy() == null
                        ? null
                        : EmployeeSummaryDto.from(entity.getCreatedBy()),
                entity.getCreatedDate(), entity.isEnabled(), entity.isVoided(),
                entity.getVoidedBy() == null ? null : EmployeeSummaryDto.from(entity.getVoidedBy()),
                entity.getVoidedAt(), entity.getVoidReason());
    }
}
