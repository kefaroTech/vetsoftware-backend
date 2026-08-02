package com.vetsoftware.app.openaccount.application.port.in;

import com.vetsoftware.app.openaccount.application.command.UpdateOpenAccountCommand;
import com.vetsoftware.app.openaccount.application.dto.OpenAccountDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface UpdateOpenAccountUseCase {
    @PreAuthorize("hasRole('SYSTEM') or "
            + "(hasAuthority('openAccount.update') and @authz.isMyCompany(#command.companyId)) or "
            + "hasRole('SYSTEM')")
    OpenAccountDto execute(UpdateOpenAccountCommand command);
}
