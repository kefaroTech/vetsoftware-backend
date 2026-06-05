package com.vetsoftware.app.generalchargeopenaccount.application.port.in;

import com.vetsoftware.app.generalchargeopenaccount.application.command.CreateGeneralChargeOpenAccountCommand;
import com.vetsoftware.app.generalchargeopenaccount.application.dto.GeneralChargeOpenAccountDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface CreateGeneralChargeOpenAccountUseCase {
    @PreAuthorize("hasAuthority('admin.all') or "
        + "(hasAuthority('chargeOpenAccount.create') and @authz.isMyCompany(#command.companyId)) or "
        + "hasRole('SYSTEM')")
    GeneralChargeOpenAccountDto execute(CreateGeneralChargeOpenAccountCommand command);
}
