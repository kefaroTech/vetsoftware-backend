package com.vetsoftware.app.prescription.application.port.in;

import org.springframework.security.access.prepost.PreAuthorize;

public interface ExportPrescriptionUseCase {
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('prescription.read') and @authz.isMyCompany(#companyId))")
    byte[] execute(Long prescriptionId, Long companyId, Long employeeId);
}
