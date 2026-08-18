package com.vetsoftware.app.surgery.application.port.in;

import com.vetsoftware.app.surgery.application.dto.SurgeryDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ReactivateSurgeryUseCase {
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('surgery.update')"
            + " and @authz.isMyCompany(#companyId))")
    SurgeryDto execute(Long id, Long companyId);
}
