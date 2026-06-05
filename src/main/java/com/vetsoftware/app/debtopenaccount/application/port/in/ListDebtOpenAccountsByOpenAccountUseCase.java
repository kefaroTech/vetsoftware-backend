package com.vetsoftware.app.debtopenaccount.application.port.in;

import com.vetsoftware.app.debtopenaccount.application.dto.DebtOpenAccountDto;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListDebtOpenAccountsByOpenAccountUseCase {
    @PreAuthorize("hasAuthority('admin.all') or "
        + "(hasAuthority('debtOpenAccount.read') and @authz.isMyCompany(#companyId)) or "
        + "hasRole('SYSTEM')")
    List<DebtOpenAccountDto> listByOpenAccount(Long openAccountId, Long companyId);
}
