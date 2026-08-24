package com.vetsoftware.app.entitlement.application.dto;

import com.vetsoftware.app.entitlement.domain.CompanyCapacity;
import java.time.LocalDateTime;

/**
 * Un contador contratado. {@code exhausted} se expone calculado porque es la
 * pregunta que hace la interfaz --"ofrezco ya la ampliacion?"-- y no un dato
 * mas de la fila.
 */
public record CompanyCapacityDto(Long id, Long companyId, String capacityUnit, int limitQuantity,
        int usedQuantity, boolean exhausted, Long subscriptionId, LocalDateTime recalculatedAt) {

    public static CompanyCapacityDto from(CompanyCapacity capacity) {
        return new CompanyCapacityDto(capacity.getId(), capacity.getCompanyId(),
                capacity.getUnit().name(), capacity.getLimitQuantity(), capacity.getUsedQuantity(),
                capacity.isExhausted(), capacity.getSubscriptionId(), capacity.getRecalculatedAt());
    }
}
