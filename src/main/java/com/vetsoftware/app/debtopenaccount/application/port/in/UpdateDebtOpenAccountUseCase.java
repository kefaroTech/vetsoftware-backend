package com.vetsoftware.app.debtopenaccount.application.port.in;

import com.vetsoftware.app.debtopenaccount.application.command.UpdateDebtOpenAccountCommand;
import com.vetsoftware.app.debtopenaccount.application.dto.DebtOpenAccountDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface UpdateDebtOpenAccountUseCase {
    @PreAuthorize("hasAuthority('admin.all') or "
        + "(hasAuthority('debtOpenAccount.update') and @authz.isMyCompany(#command.companyId)) or "
        + "hasRole('SYSTEM')")
    DebtOpenAccountDto execute(UpdateDebtOpenAccountCommand command);
}
