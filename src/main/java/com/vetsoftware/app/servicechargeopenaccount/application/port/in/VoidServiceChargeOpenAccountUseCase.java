package com.vetsoftware.app.servicechargeopenaccount.application.port.in;

import com.vetsoftware.app.servicechargeopenaccount.application.command.VoidServiceChargeOpenAccountCommand;
import com.vetsoftware.app.servicechargeopenaccount.application.dto.ServiceChargeOpenAccountDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface VoidServiceChargeOpenAccountUseCase {
    @PreAuthorize("hasRole('SYSTEM') or "
        + "(hasAuthority('chargeOpenAccount.delete') and @authz.isMyCompany(#command.companyId)) or "
        + "hasRole('SYSTEM')")
    ServiceChargeOpenAccountDto execute(VoidServiceChargeOpenAccountCommand command);
}
