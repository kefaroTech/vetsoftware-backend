package com.vetsoftware.app.appointment.application.port.in;

import com.vetsoftware.app.appointment.application.command.CreateAppointmentCommand;
import com.vetsoftware.app.appointment.application.dto.AppointmentDto;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Agendar una cita.
 *
 * <p>
 * <strong>Forzar el cruce es un permiso aparte.</strong> Desactivar el control
 * de solape no es «crear una cita más»: reserva un hueco que el sistema
 * considera ocupado y deja al veterinario con dos animales a la misma hora. Con
 * solo {@code appointment.create} —el rol más bajo del módulo— cualquiera podía
 * hacerlo, tantas veces como quisiera. Ahora el flag exige además
 * {@code appointment.overlap.force}.
 *
 * <p>
 * El gate va <em>factorizado</em>, no como la alternativa del molde de
 * {@code CloseCashSessionUseCase} ({@code (#flag and X) or (!#flag and Y)}):
 * allí las dos ramas son excluyentes, aquí forzar es crear <em>y además</em>
 * saltarse el control, así que el permiso base se sigue exigiendo siempre.
 * Escrito con la forma excluyente, quien tuviera solo
 * {@code appointment.overlap.force} podría crear citas sin
 * {@code appointment.create}.
 *
 * <p>
 * {@code #command.forceOverlap} tiene que coincidir letra por letra con el
 * componente del record: un {@code #forceOverlap} suelto resolvería a
 * {@code null} en silencio y la condición quedaría siempre falsa.
 */
public interface CreateAppointmentUseCase {
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('appointment.create')"
            + " and @authz.isMyCompany(#command.companyId)"
            + " and (!#command.forceOverlap or hasAuthority('appointment.overlap.force')))")
    AppointmentDto execute(CreateAppointmentCommand command);
}
