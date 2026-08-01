package com.vetsoftware.app.appointment.application.port.in;

import org.springframework.security.access.prepost.PreAuthorize;

public interface DeleteAppointmentUseCase {
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('appointment.delete') and @authz.isMyCompany(#companyId))")
    void execute(Long id, Long companyId);
}
