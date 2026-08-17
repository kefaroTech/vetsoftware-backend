package com.vetsoftware.app.appointment.application.port.out;

import com.vetsoftware.app.appointment.domain.Appointment;
import com.vetsoftware.app.appointment.domain.AppointmentStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AppointmentRepository {
    Appointment save(Appointment appointment);

    Optional<Appointment> findByIdAndCompanyId(Long id, Long companyId);

    List<Appointment> findByFilters(Long companyId, LocalDateTime from, LocalDateTime to,
            Long employeeId, AppointmentStatus status, Long branchId);

    /**
     * Cita que se cruza con la nueva: su id y la <strong>sede</strong> en la que
     * está agendada.
     *
     * <p>
     * La sede viaja con el id porque el cruce se calcula por empresa + empleado
     * —sin sede, un veterinario no puede estar en dos sitios a la vez— pero el
     * caller solo puede <em>ver</em> las citas de las sedes que tiene asignadas.
     * Sin este dato, el 409 devolvía ids de sedes ajenas y, con
     * {@code GET /appointments/{id}} acotado solo por empresa, se reconstruía la
     * agenda completa de un veterinario en otra sede.
     */
    record Overlap(Long id, Long branchId) {
    }

    /**
     * Citas del mismo veterinario (misma empresa + mismo empleado) cuyo intervalo
     * <strong>se cruza</strong> con {@code [startAt, endAt)} (BE-17).
     *
     * <p>
     * <strong>Bloquea</strong>: el caso de uso lanza
     * {@code AppointmentOverlapException} (409) salvo que el command venga con el
     * flag de forzado. Ya no es «solo aviso»: antes se comparaba la igualdad exacta
     * de la hora de inicio y el conflicto se descubría con el animal en la sala.
     *
     * <p>
     * Los intervalos son <strong>semiabiertos</strong>: una cita 10:00-10:30 y otra
     * 10:30-11:00 no se cruzan. Se descartan las citas que no ocupan agenda
     * ({@code CANCELLED}, {@code NO_SHOW}); {@code COMPLETED} sí cuenta. El alcance
     * es el veterinario, no la sede.
     *
     * @param endAt
     *            fin del intervalo nuevo, ya derivado por el dominio.
     * @param defaultDurationMinutes
     *            duración con la que se resuelve la de las citas existentes que la
     *            tengan a {@code NULL} en la base.
     * @param excludeId
     *            la cita que se está editando, para que no choque consigo misma;
     *            {@code null} al crear.
     */
    List<Overlap> findOverlapping(Long companyId, Long employeeId, LocalDateTime startAt,
            LocalDateTime endAt, int defaultDurationMinutes, Long excludeId);

    void delete(Long id, Long companyId);
}
