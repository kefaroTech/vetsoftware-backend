package com.vetsoftware.app.hospitalizationprocedure.application.port.in;

import com.vetsoftware.app.hospitalizationprocedure.application.dto.HospitalizationProcedureDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface FindHospitalizationProcedureUseCase {
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('hospitalization.read') and"
            + " @authz.isMyCompany(#companyId))")
    HospitalizationProcedureDto findById(Long id, Long companyId);
}
