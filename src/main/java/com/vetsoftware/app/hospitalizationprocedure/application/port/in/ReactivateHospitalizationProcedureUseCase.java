package com.vetsoftware.app.hospitalizationprocedure.application.port.in;

import com.vetsoftware.app.hospitalizationprocedure.application.dto.HospitalizationProcedureDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ReactivateHospitalizationProcedureUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasAuthority('hospitalizationProcedure.update') or hasRole('SYSTEM')")
    HospitalizationProcedureDto execute(Long id);
}
