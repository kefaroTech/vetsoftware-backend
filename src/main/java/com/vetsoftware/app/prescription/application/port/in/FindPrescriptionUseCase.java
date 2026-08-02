package com.vetsoftware.app.prescription.application.port.in;

import com.vetsoftware.app.prescription.application.dto.PrescriptionDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface FindPrescriptionUseCase {
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('prescription.read') and @authz.isMyCompany(#companyId))")
    PrescriptionDto findById(Long id, Long companyId);
}
