package com.vetsoftware.app.debtopenaccount.application.port.in;

import org.springframework.security.access.prepost.PreAuthorize;

public interface DeleteDebtOpenAccountUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasAuthority('debtOpenAccount.delete') or "
        + "hasRole('SYSTEM')")
    void execute(Long id);
}
