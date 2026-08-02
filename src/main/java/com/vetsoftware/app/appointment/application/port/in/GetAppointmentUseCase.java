package com.vetsoftware.app.appointment.application.port.in;

import com.vetsoftware.app.appointment.application.dto.AppointmentDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface GetAppointmentUseCase {
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('appointment.read') and @authz.isMyCompany(#companyId))")
    AppointmentDto findById(Long id, Long companyId);
}
