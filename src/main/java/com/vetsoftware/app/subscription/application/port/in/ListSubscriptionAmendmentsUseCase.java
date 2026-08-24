package com.vetsoftware.app.subscription.application.port.in;

import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.subscription.application.dto.SubscriptionAmendmentDto;
import org.springframework.security.access.prepost.PreAuthorize;

/** Los otrosies de un contrato, en orden de fecha efectiva. Nunca se borran. */
public interface ListSubscriptionAmendmentsUseCase {
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('subscription.read') and"
            + " @authz.isMyCompany(#companyId))")
    PageResult<SubscriptionAmendmentDto> listAll(Long subscriptionId, Long companyId, int page,
            int pageSize);
}
