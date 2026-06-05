package com.vetsoftware.app.debtopenaccount.application.port.in;

import com.vetsoftware.app.debtopenaccount.application.dto.DebtOpenAccountDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ReactivateDebtOpenAccountUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasAuthority('debtOpenAccount.delete') or "
        + "hasRole('SYSTEM')")
    DebtOpenAccountDto execute(Long id);
}
