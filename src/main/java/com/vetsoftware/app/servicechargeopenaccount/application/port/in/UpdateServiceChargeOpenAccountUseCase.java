package com.vetsoftware.app.servicechargeopenaccount.application.port.in;

import com.vetsoftware.app.servicechargeopenaccount.application.command.UpdateServiceChargeOpenAccountCommand;
import com.vetsoftware.app.servicechargeopenaccount.application.dto.ServiceChargeOpenAccountDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface UpdateServiceChargeOpenAccountUseCase {
    @PreAuthorize("hasAuthority('admin.all') or "
        + "(hasAuthority('chargeOpenAccount.update') and @authz.isMyCompany(#command.companyId)) or "
        + "hasRole('SYSTEM')")
    ServiceChargeOpenAccountDto execute(UpdateServiceChargeOpenAccountCommand command);
}
