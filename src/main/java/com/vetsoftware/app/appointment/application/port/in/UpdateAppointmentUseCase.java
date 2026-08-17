package com.vetsoftware.app.appointment.application.port.in;

import com.vetsoftware.app.appointment.application.command.UpdateAppointmentCommand;
import com.vetsoftware.app.appointment.application.dto.AppointmentDto;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Editar una cita.
 *
 * <p>
 * Forzar el cruce exige {@code appointment.overlap.force} además de
 * {@code appointment.update}: saltarse el control de solape es una decisión
 * distinta de editar una cita, y no la puede tomar el rol más bajo del módulo.
 * Ver {@link CreateAppointmentUseCase} para por qué el gate va factorizado en
 * vez de con las dos ramas excluyentes del molde de
 * {@code CloseCashSessionUseCase}.
 */
public interface UpdateAppointmentUseCase {
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('appointment.update')"
            + " and @authz.isMyCompany(#command.companyId)"
            + " and (!#command.forceOverlap or hasAuthority('appointment.overlap.force')))")
    AppointmentDto execute(UpdateAppointmentCommand command);
}
