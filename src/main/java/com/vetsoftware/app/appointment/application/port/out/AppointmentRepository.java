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
                                    Long employeeId, AppointmentStatus status);

    /** IDs de citas del mismo vet a la misma hora de inicio (activas, no terminales). Solo aviso. */
    List<Long> findClashingIds(Long companyId, Long employeeId, LocalDateTime startAt, Long excludeId);

    void delete(Long id, Long companyId);
}
