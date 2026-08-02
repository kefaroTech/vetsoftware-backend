package com.vetsoftware.app.appointment.application.port.in;

import com.vetsoftware.app.appointment.application.command.RescheduleAppointmentCommand;
import com.vetsoftware.app.appointment.application.dto.AppointmentDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface RescheduleAppointmentUseCase {
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('appointment.update') and @authz.isMyCompany(#command.companyId))")
    AppointmentDto execute(RescheduleAppointmentCommand command);
}
