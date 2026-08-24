package com.vetsoftware.app.subscription.infrastructure.web.response;

import com.vetsoftware.app.subscription.domain.CapacityUnit;
import com.vetsoftware.app.subscription.domain.ItemOrigin;
import com.vetsoftware.app.subscription.domain.SubscriptionItemType;
import com.vetsoftware.app.subscription.domain.TaxTreatment;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Una linea del contrato. {@code billableQuantity} viene calculado —lo
 * contratado menos lo incluido congelado— para que el front no lo reste por su
 * cuenta y acabe cobrando de mas.
 */
public record SubscriptionItemResponse(Long id, Long companyId, Long subscriptionId,
        Long catalogItemId, String itemCode, String itemName, SubscriptionItemType itemType,
        CapacityUnit capacityUnit, int includedQuantity, TaxTreatment taxTreatment, int quantity,
        int billableQuantity, BigDecimal unitAmount, BigDecimal taxRate, LocalDate effectiveFrom,
        LocalDate effectiveTo, ItemOrigin origin, Long createdAmendmentId, Long endedAmendmentId,
        LocalDateTime createdDate, boolean enabled) {
}
