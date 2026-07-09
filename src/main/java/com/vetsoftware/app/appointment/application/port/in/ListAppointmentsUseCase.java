package com.vetsoftware.app.appointment.application.port.in;

import com.vetsoftware.app.appointment.application.dto.AppointmentDto;
import com.vetsoftware.app.appointment.application.query.ListAppointmentsQuery;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListAppointmentsUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasRole('SYSTEM') or (hasAuthority('appointment.read') and @authz.isMyCompany(#query.companyId))")
    List<AppointmentDto> execute(ListAppointmentsQuery query);
}
