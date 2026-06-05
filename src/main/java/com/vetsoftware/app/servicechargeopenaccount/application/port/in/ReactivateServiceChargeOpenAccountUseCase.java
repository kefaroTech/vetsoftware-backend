package com.vetsoftware.app.servicechargeopenaccount.application.port.in;

import com.vetsoftware.app.servicechargeopenaccount.application.dto.ServiceChargeOpenAccountDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ReactivateServiceChargeOpenAccountUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasAuthority('chargeOpenAccount.delete') or hasRole('SYSTEM')")
    ServiceChargeOpenAccountDto execute(Long id);
}
