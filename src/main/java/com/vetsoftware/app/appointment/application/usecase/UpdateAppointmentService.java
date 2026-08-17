package com.vetsoftware.app.appointment.application.usecase;

import com.vetsoftware.app.appointment.application.command.UpdateAppointmentCommand;
import com.vetsoftware.app.appointment.application.dto.AppointmentDto;
import com.vetsoftware.app.appointment.application.port.in.UpdateAppointmentUseCase;
import com.vetsoftware.app.appointment.application.port.out.AnimalQueryPort;
import com.vetsoftware.app.appointment.application.port.out.AppointmentDurationPolicyPort;
import com.vetsoftware.app.appointment.application.port.out.AppointmentRepository;
import com.vetsoftware.app.appointment.application.port.out.AppointmentRepository.Overlap;
import com.vetsoftware.app.appointment.application.port.out.EmployeeQueryPort;
import com.vetsoftware.app.appointment.application.port.out.OwnerQueryPort;
import com.vetsoftware.app.appointment.domain.AnimalRef;
import com.vetsoftware.app.appointment.domain.Appointment;
import com.vetsoftware.app.appointment.domain.AppointmentNotFoundException;
import com.vetsoftware.app.appointment.domain.AppointmentOverlapException;
import com.vetsoftware.app.appointment.domain.EmployeeRef;
import com.vetsoftware.app.appointment.domain.OwnerRef;
import io.micrometer.observation.annotation.Observed;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "appointment.update")
@Service
public class UpdateAppointmentService implements UpdateAppointmentUseCase {
    private static final Logger log = LoggerFactory.getLogger(UpdateAppointmentService.class);

    private final AppointmentRepository repository;
    private final AnimalQueryPort animalQueryPort;
    private final OwnerQueryPort ownerQueryPort;
    private final EmployeeQueryPort employeeQueryPort;
    private final AppointmentDurationPolicyPort durationPolicyPort;

    public UpdateAppointmentService(AppointmentRepository repository,
            AnimalQueryPort animalQueryPort, OwnerQueryPort ownerQueryPort,
            EmployeeQueryPort employeeQueryPort, AppointmentDurationPolicyPort durationPolicyPort) {
        this.repository = repository;
        this.animalQueryPort = animalQueryPort;
        this.ownerQueryPort = ownerQueryPort;
        this.employeeQueryPort = employeeQueryPort;
        this.durationPolicyPort = durationPolicyPort;
    }

    @Override
    @Transactional
    public AppointmentDto execute(UpdateAppointmentCommand command) {
        Appointment appointment = repository.findByIdAndCompanyId(command.id(), command.companyId())
                .orElseThrow(() -> new AppointmentNotFoundException(command.id()));

        EmployeeRef employee = employeeQueryPort
                .findByIdAndCompanyId(command.employeeId(), command.companyId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Employee not found: " + command.employeeId()));
        AnimalRef animal = command.animalId() == null
                ? null
                : animalQueryPort.findByIdAndCompanyId(command.animalId(), command.companyId())
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Animal not found: " + command.animalId()));
        OwnerRef owner = command.ownerId() == null
                ? null
                : ownerQueryPort.findByIdAndCompanyId(command.ownerId(), command.companyId())
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Owner not found: " + command.ownerId()));

        appointment.update(command.startAt(), command.durationMinutes(), command.type(),
                command.notes(), animal, owner, command.clientName(), command.clientPhone(),
                command.clientEmail(), employee);

        // BE-17: el cruce se comprueba antes del save, excluyendo la propia cita para
        // que no choque consigo misma.
        int defaultMinutes = durationPolicyPort.defaultDurationMinutes(command.companyId());
        LocalDateTime endAt = appointment.endAt(defaultMinutes);
        List<Overlap> overlaps = repository.findOverlapping(command.companyId(),
                command.employeeId(), appointment.getStartAt(), endAt, defaultMinutes,
                command.id());
        // El cruce se decide con TODOS los solapes; lo que se devuelve, solo con los
        // que el caller puede ver por sede.
        List<Long> visibleOverlapIds = AppointmentOverlaps.visibleIds(overlaps,
                command.visibleBranchIds());
        if (!overlaps.isEmpty()) {
            if (!command.forceOverlap()) {
                throw new AppointmentOverlapException(command.employeeId(), employee.name(),
                        appointment.getStartAt(), endAt, visibleOverlapIds, overlaps.size());
            }
            // El forzado no se persiste (es una decisión del momento, no un atributo
            // de la cita), así que el log es el único rastro de que alguien desactivó
            // el control de solape.
            log.warn(
                    "Appointment overlap forced on update: appointmentId={} employeeId={}"
                            + " startAt={} count={} overlappingIds={}",
                    command.id(), command.employeeId(), appointment.getStartAt(), overlaps.size(),
                    AppointmentOverlaps.allIds(overlaps));
        }

        Appointment saved = repository.save(appointment);
        // Solo puede venir no vacío si se forzó: el bloqueo ya lanzó en caso
        // contrario. Y solo con las citas visibles para el caller.
        return AppointmentDto.from(saved, visibleOverlapIds);
    }
}
