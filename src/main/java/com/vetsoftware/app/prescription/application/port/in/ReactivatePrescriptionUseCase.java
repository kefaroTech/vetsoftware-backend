package com.vetsoftware.app.prescription.application.port.in;

import com.vetsoftware.app.prescription.application.dto.PrescriptionDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ReactivatePrescriptionUseCase {
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('prescription.update')"
            + " and @authz.isMyCompany(#companyId))")
    PrescriptionDto execute(Long id, Long companyId);
}
