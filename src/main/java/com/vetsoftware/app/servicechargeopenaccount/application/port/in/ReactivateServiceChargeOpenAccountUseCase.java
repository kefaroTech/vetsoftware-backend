package com.vetsoftware.app.servicechargeopenaccount.application.port.in;

import com.vetsoftware.app.servicechargeopenaccount.application.dto.ServiceChargeOpenAccountDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ReactivateServiceChargeOpenAccountUseCase {
    @PreAuthorize("hasRole('SYSTEM') or "
            + "(hasAuthority('chargeOpenAccount.delete') and @authz.isMyCompany(#companyId)) or "
            + "hasRole('SYSTEM')")
    ServiceChargeOpenAccountDto execute(Long id, Long companyId);
}
