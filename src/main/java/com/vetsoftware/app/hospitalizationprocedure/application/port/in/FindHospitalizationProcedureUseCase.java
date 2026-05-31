package com.vetsoftware.app.hospitalizationprocedure.application.port.in;

import com.vetsoftware.app.hospitalizationprocedure.application.dto.HospitalizationProcedureDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface FindHospitalizationProcedureUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasAuthority('hospitalizationProcedure.read') or hasRole('SYSTEM')")
    HospitalizationProcedureDto findById(Long id);
}
