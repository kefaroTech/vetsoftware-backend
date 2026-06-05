package com.vetsoftware.app.generalchargeopenaccount.application.port.in;

import com.vetsoftware.app.generalchargeopenaccount.application.command.UpdateGeneralChargeOpenAccountCommand;
import com.vetsoftware.app.generalchargeopenaccount.application.dto.GeneralChargeOpenAccountDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface UpdateGeneralChargeOpenAccountUseCase {
    @PreAuthorize("hasAuthority('admin.all') or "
        + "(hasAuthority('generalChargeOpenAccount.update') and @authz.isMyCompany(#command.companyId)) or "
        + "hasRole('SYSTEM')")
    GeneralChargeOpenAccountDto execute(UpdateGeneralChargeOpenAccountCommand command);
}
