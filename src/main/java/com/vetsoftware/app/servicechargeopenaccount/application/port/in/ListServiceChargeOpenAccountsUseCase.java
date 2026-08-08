package com.vetsoftware.app.servicechargeopenaccount.application.port.in;

import com.vetsoftware.app.servicechargeopenaccount.application.dto.ServiceChargeOpenAccountDto;
import com.vetsoftware.app.servicechargeopenaccount.application.dto.PageResult;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListServiceChargeOpenAccountsUseCase {
    @PreAuthorize("hasRole('SYSTEM')")
    PageResult<ServiceChargeOpenAccountDto> listAll(Long companyId, int page, int pageSize);
}
