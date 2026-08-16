package com.vetsoftware.app.debtopenaccount.application.port.in;

import com.vetsoftware.app.debtopenaccount.application.dto.DebtOpenAccountDto;
import com.vetsoftware.app.shared.pagination.PageResult;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListDebtOpenAccountsUseCase {
    @PreAuthorize("hasRole('SYSTEM')")
    PageResult<DebtOpenAccountDto> listAll(Long companyId, int page, int pageSize);
}
