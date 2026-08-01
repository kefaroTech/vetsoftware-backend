package com.vetsoftware.app.debtopenaccount.application.port.in;

import com.vetsoftware.app.debtopenaccount.application.dto.DebtOpenAccountDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface FindDebtOpenAccountUseCase {
    @PreAuthorize("hasRole('SYSTEM') or hasAuthority('debtOpenAccount.read')")
    DebtOpenAccountDto findById(Long id, Long companyId);
}
