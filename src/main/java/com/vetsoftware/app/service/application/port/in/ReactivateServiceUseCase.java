package com.vetsoftware.app.service.application.port.in;

import com.vetsoftware.app.service.application.dto.ServiceDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ReactivateServiceUseCase {
    @PreAuthorize("hasRole('SYSTEM') or "
            + "(hasAuthority('service.delete') and @authz.isMyCompany(#companyId)"
            + " and @authz.requireModuleWritable('SERVICES'))")
    ServiceDto execute(Long id, Long companyId);
}
