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
        // Issue #241: el mismo lock del alta (#114), el mismo puerto y el mismo
        // punto del metodo —PRIMERA sentencia, antes de leer la cita y antes de
        // resolver al veterinario—. Editar repetia el leer-y-escribir que el #114
        // cerro solo en la creacion: dos ediciones concurrentes sobre la agenda del
        // mismo profesional leian las dos un hueco libre y guardaban las dos.
        //
        // Desde el #240 esto ya no tiene red debajo: una cita forzada deja su
        // active_slot_employee_id a NULL, no compite por
        // uq_appointments_active_employee_start y la base deja de arbitrar incluso
        // el solape EXACTO. El unico arbitro que queda es el findOverlapping de
        // abajo, y solo sirve serializado.
        //
        // Se bloquea al veterinario DESTINO —command.employeeId()—, y solo a el,
        // aunque el PUT pueda cambiar de profesional. Motivo: una agenda solo gana
        // solapes por las escrituras que meten una cita EN ella, y todas —alta,
        // edicion y reprogramacion— pasan por este mismo lock; sacar una cita de la
        // agenda de origen no puede crear un cruce alli. Bloquear ademas el origen
        // exigiria leer la cita ANTES del lock —justo el patron que se esta
        // arreglando— y meteria un segundo lock por transaccion, con el
        // interbloqueo cruzado que documenta el #229. Con un unico lock por
        // transaccion no hay ciclo de espera posible y el orden total sobra.
        employeeQueryPort.lockForOverlapCheck(command.employeeId(), command.companyId());

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
            log.warn(
                    "Appointment overlap forced on update: appointmentId={} employeeId={}"
                            + " startAt={} count={} overlappingIds={}",
                    command.id(), command.employeeId(), appointment.getStartAt(), overlaps.size(),
                    AppointmentOverlaps.allIds(overlaps));
        }

        // Issue #240: el forzado se persiste para que esta cita no compita por la
        // clave de uq_appointments_active_employee_start y pueda quedarse encima de
        // la que ya ocupaba el hueco. En el PUT importa además el sentido contrario:
        // si esta edición mueve una cita antes forzada a un hueco libre, el flag
        // vuelve a false y la cita recupera su reserva frente a la carrera. Por eso
        // se asigna siempre, no solo dentro del if.
        appointment.markOverlapForced(!overlaps.isEmpty() && command.forceOverlap());

        Appointment saved = repository.save(appointment);
        // Solo puede venir no vacío si se forzó: el bloqueo ya lanzó en caso
        // contrario. Y solo con las citas visibles para el caller.
        return AppointmentDto.from(saved, visibleOverlapIds);
    }
}
