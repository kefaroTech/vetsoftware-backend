package com.vetsoftware.app.subscription.application.port.in;

import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.subscription.application.dto.SubscriptionDto;
import org.springframework.security.access.prepost.PreAuthorize;

/** El historial de contratos de una empresa. Acotado siempre. */
public interface ListSubscriptionsByCompanyUseCase {
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('subscription.read') and"
            + " @authz.isMyCompany(#companyId))")
    PageResult<SubscriptionDto> listByCompany(Long companyId, int page, int pageSize);
}
