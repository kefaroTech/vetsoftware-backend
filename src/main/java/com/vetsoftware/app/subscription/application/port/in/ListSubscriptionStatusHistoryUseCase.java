package com.vetsoftware.app.subscription.application.port.in;

import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.subscription.application.dto.SubscriptionStatusChangeDto;
import org.springframework.security.access.prepost.PreAuthorize;

/** La pelicula del contrato: por que esta cuenta esta en solo lectura. */
public interface ListSubscriptionStatusHistoryUseCase {
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('subscription.read') and"
            + " @authz.isMyCompany(#companyId))")
    PageResult<SubscriptionStatusChangeDto> listAll(Long subscriptionId, Long companyId, int page,
            int pageSize);
}
