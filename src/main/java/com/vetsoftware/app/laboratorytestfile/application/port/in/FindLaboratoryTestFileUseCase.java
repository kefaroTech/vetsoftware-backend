package com.vetsoftware.app.laboratorytestfile.application.port.in;

import com.vetsoftware.app.laboratorytestfile.application.dto.LaboratoryTestFileDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface FindLaboratoryTestFileUseCase {
    @PreAuthorize("hasRole('SYSTEM') or ((hasAuthority('admin.all') or hasAuthority('laboratoryTest.read')) and @authz.isMyCompany(#companyId))")
    LaboratoryTestFileDto findById(Long id, Long companyId);
}
