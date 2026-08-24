package com.vetsoftware.app.pricelist.application.command;

import com.vetsoftware.app.pricelist.domain.BillingCycle;
import com.vetsoftware.app.pricelist.domain.TaxTreatment;
import java.math.BigDecimal;

/**
 * Ni la lista ni el articulo viajan: reapuntar un precio a otra lista o a otro
 * articulo no es editarlo, es crear otro.
 */
public record UpdateCatalogPriceCommand(Long id, BillingCycle billingCycle, int tierMin,
        Integer tierMax, int includedQuantity, BigDecimal unitAmount, BigDecimal setupAmount,
        BigDecimal taxRate, TaxTreatment taxTreatment) {
}
