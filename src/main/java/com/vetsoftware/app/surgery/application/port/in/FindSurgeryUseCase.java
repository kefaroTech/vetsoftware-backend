package com.vetsoftware.app.surgery.application.port.in;

import com.vetsoftware.app.surgery.application.dto.SurgeryDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface FindSurgeryUseCase {
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('surgery.read') and @authz.isMyCompany(#companyId))")
    SurgeryDto findById(Long id, Long companyId);
}
