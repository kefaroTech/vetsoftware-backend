package com.vetsoftware.app.servicechargeopenaccount.application.port.in;

import com.vetsoftware.app.servicechargeopenaccount.application.dto.ServiceChargeOpenAccountDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface FindServiceChargeOpenAccountUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasAuthority('chargeOpenAccount.read') or hasRole('SYSTEM')")
    ServiceChargeOpenAccountDto findById(Long id, Long companyId);
}
