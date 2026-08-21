package com.vetsoftware.app.appointment.application.usecase;

import com.vetsoftware.app.appointment.application.command.RescheduleAppointmentCommand;
import com.vetsoftware.app.appointment.application.dto.AppointmentDto;
import com.vetsoftware.app.appointment.application.port.in.RescheduleAppointmentUseCase;
import com.vetsoftware.app.appointment.application.port.out.AppointmentDurationPolicyPort;
import com.vetsoftware.app.appointment.application.port.out.AppointmentRepository;
import com.vetsoftware.app.appointment.application.port.out.AppointmentRepository.Overlap;
import com.vetsoftware.app.appointment.application.port.out.EmployeeQueryPort;
import com.vetsoftware.app.appointment.domain.Appointment;
import com.vetsoftware.app.appointment.domain.AppointmentNotFoundException;
import com.vetsoftware.app.appointment.domain.AppointmentOverlapException;
import com.vetsoftware.app.appointment.domain.EmployeeRef;
import io.micrometer.observation.annotation.Observed;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "appointment.reschedule")
@Service
public class RescheduleAppointmentService implements RescheduleAppointmentUseCase {
    private static final Logger log = LoggerFactory.getLogger(RescheduleAppointmentService.class);

    private final AppointmentRepository repository;
    private final EmployeeQueryPort employeeQueryPort;
    private final AppointmentDurationPolicyPort durationPolicyPort;

    public RescheduleAppointmentService(AppointmentRepository repository,
            EmployeeQueryPort employeeQueryPort, AppointmentDurationPolicyPort durationPolicyPort) {
        this.repository = repository;
        this.employeeQueryPort = employeeQueryPort;
        this.durationPolicyPort = durationPolicyPort;
    }

    @Override
    @Transactional
    public AppointmentDto execute(RescheduleAppointmentCommand command) {
        // Issue #241: identico al alta (#114) y a la edicion —mismo puerto, misma
        // primera sentencia, antes de leer la cita y antes de resolver al
        // veterinario—. Reprogramar es el verbo mas expuesto de los tres: mover
        // citas es lo que hace una recepcion con la agenda llena, y hasta aqui lo
        // hacia con el mismo leer-y-escribir sin serializar que el #114 corrigio
        // solo en la creacion.
        //
        // Y desde el #240 la base ya no cubre la espalda: una cita forzada renuncia
        // a su hueco en uq_appointments_active_employee_start, asi que dos
        // reprogramaciones pueden apilarse encima de ella sin que nada las frene
        // salvo el findOverlapping de abajo.
        //
        // Solo se bloquea al veterinario DESTINO —command.employeeId()— aunque el
        // PATCH pueda cambiar de profesional: la agenda de origen no gana solapes
        // por perder una cita, conocer su id obligaria a leer la cita antes del
        // lock y un segundo lock por transaccion abriria el interbloqueo cruzado
        // del #229. El razonamiento completo esta en UpdateAppointmentService y en
        // el javadoc de EmployeeQueryPort.lockForOverlapCheck.
        employeeQueryPort.lockForOverlapCheck(command.employeeId(), command.companyId());

        Appointment appointment = repository.findByIdAndCompanyId(command.id(), command.companyId())
                .orElseThrow(() -> new AppointmentNotFoundException(command.id()));
        EmployeeRef employee = employeeQueryPort
                .findByIdAndCompanyId(command.employeeId(), command.companyId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Employee not found: " + command.employeeId()));

        appointment.reschedule(command.startAt(), command.durationMinutes(), employee);

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
                    "Appointment overlap forced on reschedule: appointmentId={} employeeId={}"
                            + " startAt={} count={} overlappingIds={}",
                    command.id(), command.employeeId(), appointment.getStartAt(), overlaps.size(),
                    AppointmentOverlaps.allIds(overlaps));
        }

        // Issue #240: mismo criterio que en el alta y la edición. Reprogramar es el
        // verbo donde más se nota: mover una cita al hueco de otra es exactamente lo
        // que la recepción hace cuando encaja una urgencia, y sin este flag el save
        // moría contra uq_appointments_active_employee_start. Y al revés: sacar de un
        // hueco compartido una cita antes forzada la devuelve a false, con lo que
        // vuelve a reservar su nuevo hueco.
        appointment.markOverlapForced(!overlaps.isEmpty() && command.forceOverlap());

        Appointment saved = repository.save(appointment);
        // Solo puede venir no vacío si se forzó: el bloqueo ya lanzó en caso
        // contrario. Y solo con las citas visibles para el caller.
        return AppointmentDto.from(saved, visibleOverlapIds);
    }
}
