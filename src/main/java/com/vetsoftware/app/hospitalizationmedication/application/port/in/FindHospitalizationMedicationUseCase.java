package com.vetsoftware.app.hospitalizationmedication.application.port.in;

import com.vetsoftware.app.hospitalizationmedication.application.dto.HospitalizationMedicationDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface FindHospitalizationMedicationUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasAuthority('hospitalization.read') or hasRole('SYSTEM')")
    HospitalizationMedicationDto findById(Long id);
}
