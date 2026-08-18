package com.vetsoftware.app.surgerytype.application.port.in;

import com.vetsoftware.app.surgerytype.application.dto.SurgeryTypeDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ReactivateSurgeryTypeUseCase {
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('surgery.update')"
            + " and @authz.isMyCompany(#companyId))")
    SurgeryTypeDto execute(Long id, Long companyId);
}
