package com.vetsoftware.app.appointment.application.port.in;

import com.vetsoftware.app.appointment.application.command.CreateAppointmentCommand;
import com.vetsoftware.app.appointment.application.dto.AppointmentDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface CreateAppointmentUseCase {
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('appointment.create') and @authz.isMyCompany(#command.companyId))")
    AppointmentDto execute(CreateAppointmentCommand command);
}
