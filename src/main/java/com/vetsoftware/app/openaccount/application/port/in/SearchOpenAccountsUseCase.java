package com.vetsoftware.app.openaccount.application.port.in;

import com.vetsoftware.app.openaccount.application.command.SearchOpenAccountsCommand;
import com.vetsoftware.app.openaccount.application.dto.OpenAccountDto;
import com.vetsoftware.app.openaccount.application.dto.PageResult;
import org.springframework.security.access.prepost.PreAuthorize;

public interface SearchOpenAccountsUseCase {
    @PreAuthorize("hasAuthority('admin.all') or "
        + "(hasAuthority('openAccount.read') and @authz.isMyCompany(#command.companyId))")
    PageResult<OpenAccountDto> execute(SearchOpenAccountsCommand command);
}
