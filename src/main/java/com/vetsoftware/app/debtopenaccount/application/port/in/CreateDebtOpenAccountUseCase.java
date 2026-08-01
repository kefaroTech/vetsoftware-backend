package com.vetsoftware.app.debtopenaccount.application.port.in;

import com.vetsoftware.app.debtopenaccount.application.command.CreateDebtOpenAccountCommand;
import com.vetsoftware.app.debtopenaccount.application.dto.DebtOpenAccountDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface CreateDebtOpenAccountUseCase {
    @PreAuthorize("hasRole('SYSTEM') or "
        + "(hasAuthority('debtOpenAccount.create') and @authz.isMyCompany(#command.companyId)) or "
        + "hasRole('SYSTEM')")
    DebtOpenAccountDto execute(CreateDebtOpenAccountCommand command);
}
