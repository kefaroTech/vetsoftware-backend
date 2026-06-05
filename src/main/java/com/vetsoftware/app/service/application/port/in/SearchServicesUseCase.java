package com.vetsoftware.app.service.application.port.in;

import com.vetsoftware.app.service.application.command.SearchServicesCommand;
import com.vetsoftware.app.service.application.dto.PageResult;
import com.vetsoftware.app.service.application.dto.ServiceDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface SearchServicesUseCase {
    @PreAuthorize("hasAuthority('admin.all') or "
        + "(hasAuthority('service.read') and @authz.isMyCompany(#command.companyId))")
    PageResult<ServiceDto> execute(SearchServicesCommand command);
}
