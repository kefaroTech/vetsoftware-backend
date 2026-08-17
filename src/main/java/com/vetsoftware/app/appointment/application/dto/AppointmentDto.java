package com.vetsoftware.app.appointment.application.dto;

import com.vetsoftware.app.appointment.domain.Appointment;
import com.vetsoftware.app.appointment.domain.AppointmentStatus;
import com.vetsoftware.app.appointment.domain.AppointmentType;
import java.time.LocalDateTime;
import java.util.List;

/**
 * @param durationMinutes
 *            duración propia de la cita. {@code null} significa <em>hereda la
 *            duración por defecto de la empresa</em> (ajuste
 *            {@code appointment.default_duration_minutes}, y en su defecto 30
 *            minutos). No se expone el fin calculado: es derivado
 *            ({@code startAt + duración}) y almacenarlo o publicarlo como
 *            segundo dato es la vía rápida a que los dos se desincronicen.
 * @param overlappingAppointmentIds
 *            <strong>Cambió de significado con BE-17.</strong> Antes era un
 *            aviso no bloqueante: la cita se guardaba y aquí venían las citas
 *            del mismo veterinario a la misma hora exacta. Ahora el cruce
 *            <em>bloquea</em> con 409 {@code APPOINTMENT_OVERLAP}, así que este
 *            array solo llega no vacío cuando la operación se guardó
 *            <em>forzada</em> ({@code forceOverlap: true}): son las citas con
 *            las que el usuario decidió convivir. En cualquier otra respuesta
 *            correcta viene vacío, nunca {@code null}.
 *            <p>
 *            <strong>Solo las citas dentro del alcance de sede del
 *            caller.</strong> El cruce se calcula por empresa + empleado, sin
 *            sede, pero el listado de citas sí está acotado por sede: devolver
 *            aquí el id de una cita de otra sede la hacía legible entera
 *            —cliente, teléfono, correo, animal— por
 *            {@code GET /appointments/{id}}. Un forzado cuyos únicos cruces
 *            sean de sedes ajenas devuelve por tanto un array vacío.
 *            <p>
 *            Se conserva —en vez de eliminarlo— porque el front tiene copy y
 *            lógica colgando de él y sigue siendo la única forma de saber, tras
 *            un forzado, con qué se solapó lo que acaba de guardarse. Lo que
 *            debe cambiar en el front es el <em>tono</em>: de «ojo, puede haber
 *            un cruce» a «reservado sobre estas citas».
 */
public record AppointmentDto(Long id, LocalDateTime startAt, Integer durationMinutes,
        AppointmentType type, AppointmentStatus status, String notes, String cancellationReason,
        AnimalSummaryDto animal, OwnerSummaryDto owner, String clientName, String clientPhone,
        String clientEmail, EmployeeSummaryDto employee, BranchSummaryDto branch, long version,
        boolean enabled, LocalDateTime createdDate, List<Long> overlappingAppointmentIds) {

    public static AppointmentDto from(Appointment a) {
        return from(a, List.of());
    }

    public static AppointmentDto from(Appointment a, List<Long> overlaps) {
        return new AppointmentDto(a.getId(), a.getStartAt(), a.getDurationMinutes(), a.getType(),
                a.getStatus(), a.getNotes(), a.getCancellationReason(),
                a.getAnimal() == null
                        ? null
                        : new AnimalSummaryDto(a.getAnimal().id(), a.getAnimal().name(),
                                a.getAnimal().code()),
                a.getOwner() == null
                        ? null
                        : new OwnerSummaryDto(a.getOwner().id(), a.getOwner().name()),
                a.getClientName(), a.getClientPhone(), a.getClientEmail(),
                new EmployeeSummaryDto(a.getEmployee().id(), a.getEmployee().name()),
                new BranchSummaryDto(a.getBranch().id(), a.getBranch().name(),
                        a.getBranch().code()),
                a.getVersion(), a.isEnabled(), a.getCreatedDate(),
                overlaps == null ? List.of() : overlaps);
    }
}
