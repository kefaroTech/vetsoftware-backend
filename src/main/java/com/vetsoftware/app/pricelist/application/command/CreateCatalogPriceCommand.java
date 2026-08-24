package com.vetsoftware.app.pricelist.application.command;

import com.vetsoftware.app.pricelist.domain.BillingCycle;
import com.vetsoftware.app.pricelist.domain.TaxTreatment;
import java.math.BigDecimal;

public record CreateCatalogPriceCommand(Long priceListId, Long catalogItemId,
        BillingCycle billingCycle, int tierMin, Integer tierMax, int includedQuantity,
        BigDecimal unitAmount, BigDecimal setupAmount, BigDecimal taxRate,
        TaxTreatment taxTreatment) {
}
