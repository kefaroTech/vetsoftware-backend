package com.vetsoftware.app.subscription.application.dto;

import com.vetsoftware.app.subscription.domain.CapacityUnit;
import com.vetsoftware.app.subscription.domain.ItemOrigin;
import com.vetsoftware.app.subscription.domain.SubscriptionItem;
import com.vetsoftware.app.subscription.domain.SubscriptionItemType;
import com.vetsoftware.app.subscription.domain.TaxTreatment;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Una linea del expediente. Lleva {@code billableQuantity} ya calculado por el
 * dominio —lo contratado menos lo incluido congelado— para que ninguna capa de
 * arriba tenga que volver a restarlo y equivocarse.
 */
public record SubscriptionItemDto(Long id, Long companyId, Long subscriptionId, Long catalogItemId,
        String itemCode, String itemName, SubscriptionItemType itemType, CapacityUnit capacityUnit,
        int includedQuantity, TaxTreatment taxTreatment, int quantity, int billableQuantity,
        BigDecimal unitAmount, BigDecimal taxRate, LocalDate effectiveFrom, LocalDate effectiveTo,
        ItemOrigin origin, Long createdAmendmentId, Long endedAmendmentId,
        LocalDateTime createdDate, boolean enabled) {

    public static SubscriptionItemDto from(SubscriptionItem item) {
        return new SubscriptionItemDto(item.getId(), item.getCompanyId(), item.getSubscriptionId(),
                item.getCatalogItemId(), item.getItemCode(), item.getItemName(), item.getItemType(),
                item.getCapacityUnit(), item.getIncludedQuantity(), item.getTaxTreatment(),
                item.getQuantity(), item.billableQuantity(), item.getUnitAmount(),
                item.getTaxRate(), item.getPeriod().from(), item.getPeriod().to(), item.getOrigin(),
                item.getCreatedAmendmentId(), item.getEndedAmendmentId(), item.getCreatedDate(),
                item.isEnabled());
    }
}
