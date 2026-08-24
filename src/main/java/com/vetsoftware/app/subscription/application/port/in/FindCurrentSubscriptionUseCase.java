package com.vetsoftware.app.subscription.application.port.in;

import com.vetsoftware.app.subscription.application.dto.SubscriptionDto;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * El contrato vigente de una empresa. Vigente NO es {@code status = 'ACTIVE'}:
 * son los cuatro estados de {@code SubscriptionStatus.CURRENT}, que es lo mismo
 * que alimenta la columna generada {@code active_marker}.
 */
public interface FindCurrentSubscriptionUseCase {
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('subscription.read') and"
            + " @authz.isMyCompany(#companyId))")
    SubscriptionDto findCurrent(Long companyId);
}
