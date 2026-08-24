package com.vetsoftware.app.dunning.application.port.in;

import com.vetsoftware.app.dunning.application.dto.DunningEventDto;
import com.vetsoftware.app.shared.pagination.PageResult;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListDunningEventsBySubscriptionUseCase {

    /**
     * El expediente en orden. Acotar por {@code subscriptionId} <strong>no</strong>
     * basta -un contrato es de alguien-, asi que el {@code companyId} viaja y
     * filtra tambien ({@code LISTADOS_SIN_EMPRESA_SOLO_SYSTEM}).
     */
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('dunningEvent.read')"
            + " and @authz.isMyCompany(#companyId))")
    PageResult<DunningEventDto> listBySubscription(Long subscriptionId, Long companyId, int page,
            int pageSize);
}
