package com.vetsoftware.app.openaccount.application.port.in;

import com.vetsoftware.app.openaccount.application.command.CreateOpenAccountCommand;
import com.vetsoftware.app.openaccount.application.dto.OpenAccountDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface CreateOpenAccountUseCase {
    @PreAuthorize("hasAuthority('admin.all') or "
        + "(hasAuthority('openAccount.create') and @authz.isMyCompany(#command.companyId)) or hasRole('SYSTEM')")
    OpenAccountDto execute(CreateOpenAccountCommand command);
}
