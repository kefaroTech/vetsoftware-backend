package com.vetsoftware.app.subscription.application.dto;

import com.vetsoftware.app.subscription.domain.CapacityUnit;
import com.vetsoftware.app.subscription.domain.SubscriptionItemType;
import com.vetsoftware.app.subscription.domain.TaxTreatment;
import java.math.BigDecimal;

/** Valores comerciales resueltos en servidor antes de firmar una línea. */
public record SubscriptionItemSnapshot(Long catalogItemId, String itemCode, String itemName,
        SubscriptionItemType itemType, CapacityUnit capacityUnit, int includedQuantity,
        TaxTreatment taxTreatment, int quantity, BigDecimal unitAmount, BigDecimal taxRate) {
}
