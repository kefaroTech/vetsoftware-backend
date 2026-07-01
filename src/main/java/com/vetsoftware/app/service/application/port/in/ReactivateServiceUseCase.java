package com.vetsoftware.app.service.application.port.in;

import com.vetsoftware.app.service.application.dto.ServiceDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ReactivateServiceUseCase {
    @PreAuthorize("hasAuthority('admin.all') or "
        + "(hasAuthority('service.delete') and @authz.isMyCompany(#companyId))")
    ServiceDto execute(Long id, Long companyId);
}
