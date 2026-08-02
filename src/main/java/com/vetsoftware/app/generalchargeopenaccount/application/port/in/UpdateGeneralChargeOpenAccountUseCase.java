package com.vetsoftware.app.generalchargeopenaccount.application.port.in;

import com.vetsoftware.app.generalchargeopenaccount.application.command.UpdateGeneralChargeOpenAccountCommand;
import com.vetsoftware.app.generalchargeopenaccount.application.dto.GeneralChargeOpenAccountDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface UpdateGeneralChargeOpenAccountUseCase {
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('chargeOpenAccount.update') and"
            + " @authz.isMyCompany(#command.companyId)) or hasRole('SYSTEM')")
    GeneralChargeOpenAccountDto execute(UpdateGeneralChargeOpenAccountCommand command);
}
