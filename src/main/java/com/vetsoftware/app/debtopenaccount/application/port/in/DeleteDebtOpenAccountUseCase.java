package com.vetsoftware.app.debtopenaccount.application.port.in;

import com.vetsoftware.app.debtopenaccount.application.command.DeleteDebtOpenAccountCommand;
import org.springframework.security.access.prepost.PreAuthorize;

public interface DeleteDebtOpenAccountUseCase {
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('debtOpenAccount.delete') and"
            + " @authz.isMyCompany(#command.companyId))")
    void execute(DeleteDebtOpenAccountCommand command);
}
