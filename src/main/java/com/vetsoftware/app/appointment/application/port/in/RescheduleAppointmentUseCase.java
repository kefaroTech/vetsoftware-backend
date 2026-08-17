package com.vetsoftware.app.appointment.application.port.in;

import com.vetsoftware.app.appointment.application.command.RescheduleAppointmentCommand;
import com.vetsoftware.app.appointment.application.dto.AppointmentDto;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Reprogramar una cita (hora y/o duración).
 *
 * <p>
 * Forzar el cruce exige {@code appointment.overlap.force} además de
 * {@code appointment.update}. Ver {@link CreateAppointmentUseCase} para por qué
 * el gate va factorizado en vez de con las dos ramas excluyentes del molde de
 * {@code CloseCashSessionUseCase}.
 */
public interface RescheduleAppointmentUseCase {
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('appointment.update')"
            + " and @authz.isMyCompany(#command.companyId)"
            + " and (!#command.forceOverlap or hasAuthority('appointment.overlap.force')))")
    AppointmentDto execute(RescheduleAppointmentCommand command);
}
