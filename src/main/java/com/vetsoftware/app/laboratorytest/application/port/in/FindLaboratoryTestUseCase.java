package com.vetsoftware.app.laboratorytest.application.port.in;

import com.vetsoftware.app.laboratorytest.application.dto.LaboratoryTestDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface FindLaboratoryTestUseCase {
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('laboratoryTest.read') and"
            + " @authz.isMyCompany(#companyId))")
    LaboratoryTestDto findById(Long id, Long companyId);
}
