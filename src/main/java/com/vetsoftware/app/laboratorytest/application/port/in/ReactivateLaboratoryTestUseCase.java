package com.vetsoftware.app.laboratorytest.application.port.in;

import com.vetsoftware.app.laboratorytest.application.dto.LaboratoryTestDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ReactivateLaboratoryTestUseCase {
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('laboratoryTest.update')"
            + " and @authz.isMyCompany(#companyId))")
    LaboratoryTestDto execute(Long id, Long companyId);
}
