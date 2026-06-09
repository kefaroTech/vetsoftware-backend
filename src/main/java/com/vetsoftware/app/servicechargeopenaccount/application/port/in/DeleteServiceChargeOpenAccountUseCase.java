package com.vetsoftware.app.servicechargeopenaccount.application.port.in;

import org.springframework.security.access.prepost.PreAuthorize;

public interface DeleteServiceChargeOpenAccountUseCase {
    @PreAuthorize("hasAuthority('admin.all') or "
        + "(hasAuthority('chargeOpenAccount.delete') and @authz.isMyCompany(#companyId)) or "
        + "hasRole('SYSTEM')")
    void execute(Long id, Long companyId);
}
