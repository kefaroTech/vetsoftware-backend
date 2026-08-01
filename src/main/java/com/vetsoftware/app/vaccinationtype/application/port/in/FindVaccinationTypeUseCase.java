package com.vetsoftware.app.vaccinationtype.application.port.in;

import com.vetsoftware.app.vaccinationtype.application.dto.VaccinationTypeDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface FindVaccinationTypeUseCase {
    @PreAuthorize("hasRole('SYSTEM') or ((hasAuthority('admin.all') or hasAuthority('vaccination.read')) and @authz.isMyCompany(#companyId))")
    VaccinationTypeDto findById(Long id, Long companyId);
}
