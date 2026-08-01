package com.vetsoftware.app.hospitalization.application.port.in;

import com.vetsoftware.app.hospitalization.application.dto.HospitalizationDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface FindHospitalizationUseCase {
    @PreAuthorize("hasRole('SYSTEM') or ((hasAuthority('admin.all') or hasAuthority('hospitalization.read')) and @authz.isMyCompany(#companyId))")
    HospitalizationDto findById(Long id, Long companyId);
}
