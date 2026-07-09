package com.vetsoftware.app.appointment.application.port.in;

import com.vetsoftware.app.appointment.application.command.CancelAppointmentCommand;
import com.vetsoftware.app.appointment.application.dto.AppointmentDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface CancelAppointmentUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasRole('SYSTEM') or (hasAuthority('appointment.cancel') and @authz.isMyCompany(#command.companyId))")
    AppointmentDto execute(CancelAppointmentCommand command);
}
