package com.vetsoftware.app.laboratorytesttype.application.port.in;

import com.vetsoftware.app.laboratorytesttype.application.dto.LaboratoryTestTypeDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ReactivateLaboratoryTestTypeUseCase {
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('laboratoryTest.update')"
            + " and @authz.isMyCompany(#companyId))")
    LaboratoryTestTypeDto execute(Long id, Long companyId);
}
