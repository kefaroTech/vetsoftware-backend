package com.vetsoftware.app.subscription.application.port.in;

import com.vetsoftware.app.subscription.application.dto.SubscriptionDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface FindSubscriptionUseCase {
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('subscription.read') and"
            + " @authz.isMyCompany(#companyId))")
    SubscriptionDto findById(Long id, Long companyId);
}
