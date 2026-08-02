package com.vetsoftware.app.debtopenaccount.application.port.in;

import org.springframework.security.access.prepost.PreAuthorize;

public interface DeleteDebtOpenAccountUseCase {
    @PreAuthorize("hasRole('SYSTEM') or "
            + "(hasAuthority('debtOpenAccount.delete') and @authz.isMyCompany(#companyId)) or "
            + "hasRole('SYSTEM')")
    void execute(Long id, Long companyId);
}
