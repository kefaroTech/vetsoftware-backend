package com.vetsoftware.app.servicechargeopenaccount.application.port.in;

import com.vetsoftware.app.servicechargeopenaccount.application.dto.ServiceChargeOpenAccountDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface FindServiceChargeOpenAccountUseCase {
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('chargeOpenAccount.read') and @authz.isMyCompany(#companyId))")
    ServiceChargeOpenAccountDto findById(Long id, Long companyId);
}
