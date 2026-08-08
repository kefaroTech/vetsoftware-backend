package com.vetsoftware.app.openaccount.application.port.in;

import com.vetsoftware.app.openaccount.application.dto.OpenAccountDto;
import com.vetsoftware.app.openaccount.application.dto.PageResult;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListOpenAccountsUseCase {
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('openAccount.read') and @authz.isMyCompany(#companyId))")
    PageResult<OpenAccountDto> listByCompany(Long companyId, Long branchId, int page, int pageSize);
}
