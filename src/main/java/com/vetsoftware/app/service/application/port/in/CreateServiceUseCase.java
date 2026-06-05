package com.vetsoftware.app.service.application.port.in;

import com.vetsoftware.app.service.application.command.CreateServiceCommand;
import com.vetsoftware.app.service.application.dto.ServiceDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface CreateServiceUseCase {
    @PreAuthorize("hasAuthority('admin.all') or "
        + "(hasAuthority('service.create') and @authz.isMyCompany(#command.companyId))")
    ServiceDto execute(CreateServiceCommand command);
}
