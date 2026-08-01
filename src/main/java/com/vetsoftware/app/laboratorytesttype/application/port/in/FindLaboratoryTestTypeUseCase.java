package com.vetsoftware.app.laboratorytesttype.application.port.in;

import com.vetsoftware.app.laboratorytesttype.application.dto.LaboratoryTestTypeDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface FindLaboratoryTestTypeUseCase {
    @PreAuthorize("hasRole('SYSTEM') or ((hasAuthority('admin.all') or hasAuthority('laboratoryTest.read')) and @authz.isMyCompany(#companyId))")
    LaboratoryTestTypeDto findById(Long id, Long companyId);
}
