package com.vetsoftware.app.entitlement.application.dto;

import com.vetsoftware.app.entitlement.domain.CompanyEntitlement;
import java.time.LocalDateTime;

/**
 * Un permiso derivado. {@code subscriptionId} y {@code subscriptionItemId} son
 * el puente de vuelta al dinero: responden "que contrato y que linea justifican
 * esto" sin salir de la fila.
 */
public record CompanyEntitlementDto(Long id, Long companyId, SubModuleSummaryDto subModule,
        String accessLevel, String source, Long subscriptionId, Long subscriptionItemId,
        LocalDateTime validFrom, LocalDateTime validUntil, LocalDateTime recalculatedAt) {

    public static CompanyEntitlementDto from(CompanyEntitlement entitlement) {
        return new CompanyEntitlementDto(entitlement.getId(), entitlement.getCompanyId(),
                SubModuleSummaryDto.from(entitlement.getSubModule()),
                entitlement.getAccessLevel().name(), entitlement.getSource().name(),
                entitlement.getSubscriptionId(), entitlement.getSubscriptionItemId(),
                entitlement.getValidFrom(), entitlement.getValidUntil(),
                entitlement.getRecalculatedAt());
    }
}
