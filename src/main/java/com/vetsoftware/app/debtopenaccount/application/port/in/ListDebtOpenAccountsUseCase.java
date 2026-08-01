package com.vetsoftware.app.debtopenaccount.application.port.in;

import com.vetsoftware.app.debtopenaccount.application.dto.DebtOpenAccountDto;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListDebtOpenAccountsUseCase {
    @PreAuthorize("hasRole('SYSTEM')")
    List<DebtOpenAccountDto> listAll(Long companyId);
}
