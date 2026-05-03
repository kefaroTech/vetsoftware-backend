package com.vetsoftware.app.hospitalization.application.port.in;

import com.vetsoftware.app.hospitalization.application.dto.HospitalizationDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface FindHospitalizationUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasRole('SYSTEM')")
    HospitalizationDto findById(Long id);
}
