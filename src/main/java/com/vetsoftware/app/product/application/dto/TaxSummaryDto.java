package com.vetsoftware.app.product.application.dto;

import com.vetsoftware.app.product.domain.TaxRef;
import java.math.BigDecimal;

public record TaxSummaryDto(Long id, String name, BigDecimal percentage) {
    public static TaxSummaryDto from(TaxRef tax) {
        return new TaxSummaryDto(tax.id(), tax.name(), tax.percentage());
    }
}
