package com.vetsoftware.app.generalchargeopenaccount.application.port.in;

import com.vetsoftware.app.generalchargeopenaccount.application.dto.GeneralChargeOpenAccountDto;
import com.vetsoftware.app.shared.pagination.PageResult;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListGeneralChargeOpenAccountsUseCase {
    @PreAuthorize("hasRole('SYSTEM')")
    PageResult<GeneralChargeOpenAccountDto> listAll(Long companyId, int page, int pageSize);
}
