package com.vetsoftware.app.debtopenaccount.application.port.in;

import com.vetsoftware.app.debtopenaccount.application.command.ReactivateDebtOpenAccountCommand;
import com.vetsoftware.app.debtopenaccount.application.dto.DebtOpenAccountDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ReactivateDebtOpenAccountUseCase {
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('debtOpenAccount.delete') and"
            + " @authz.isMyCompany(#command.companyId))")
    DebtOpenAccountDto execute(ReactivateDebtOpenAccountCommand command);
}
