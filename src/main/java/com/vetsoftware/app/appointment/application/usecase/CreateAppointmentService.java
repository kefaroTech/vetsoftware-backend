package com.vetsoftware.app.appointment.application.usecase;

import com.vetsoftware.app.appointment.application.command.CreateAppointmentCommand;
import com.vetsoftware.app.appointment.application.dto.AppointmentConfirmationData;
import com.vetsoftware.app.appointment.application.dto.AppointmentDto;
import com.vetsoftware.app.appointment.application.port.in.CreateAppointmentUseCase;
import com.vetsoftware.app.appointment.application.port.out.AnimalQueryPort;
import com.vetsoftware.app.appointment.application.port.out.AppointmentConfirmationEmailSender;
import com.vetsoftware.app.appointment.application.port.out.AppointmentDurationPolicyPort;
import com.vetsoftware.app.appointment.application.port.out.AppointmentMetrics;
import com.vetsoftware.app.appointment.application.port.out.AppointmentMetrics.Channel;
import com.vetsoftware.app.appointment.application.port.out.AppointmentRepository;
import com.vetsoftware.app.appointment.application.port.out.AppointmentRepository.Overlap;
import com.vetsoftware.app.appointment.application.port.out.BranchQueryPort;
import com.vetsoftware.app.appointment.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.appointment.application.port.out.EmployeeQueryPort;
import com.vetsoftware.app.appointment.application.port.out.OwnerQueryPort;
import com.vetsoftware.app.appointment.domain.AnimalRef;
import com.vetsoftware.app.appointment.domain.Appointment;
import com.vetsoftware.app.appointment.domain.AppointmentOverlapException;
import com.vetsoftware.app.appointment.domain.BranchRef;
import com.vetsoftware.app.appointment.domain.CompanyRef;
import com.vetsoftware.app.appointment.domain.EmployeeRef;
import com.vetsoftware.app.appointment.domain.OwnerRef;
import io.micrometer.observation.annotation.Observed;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Observed(name = "appointment.create")
@Service
public class CreateAppointmentService implements CreateAppointmentUseCase {
    private static final Logger log = LoggerFactory.getLogger(CreateAppointmentService.class);

    private final AppointmentRepository repository;
    private final AnimalQueryPort animalQueryPort;
    private final OwnerQueryPort ownerQueryPort;
    private final EmployeeQueryPort employeeQueryPort;
    private final BranchQueryPort branchQueryPort;
    private final CompanyQueryPort companyQueryPort;
    private final AppointmentConfirmationEmailSender confirmationEmailSender;
    private final AppointmentMetrics appointmentMetrics;
    private final AppointmentDurationPolicyPort durationPolicyPort;

    public CreateAppointmentService(AppointmentRepository repository,
            AnimalQueryPort animalQueryPort, OwnerQueryPort ownerQueryPort,
            EmployeeQueryPort employeeQueryPort, BranchQueryPort branchQueryPort,
            CompanyQueryPort companyQueryPort,
            AppointmentConfirmationEmailSender confirmationEmailSender,
            AppointmentMetrics appointmentMetrics,
            AppointmentDurationPolicyPort durationPolicyPort) {
        this.durationPolicyPort = durationPolicyPort;
        this.repository = repository;
        this.animalQueryPort = animalQueryPort;
        this.ownerQueryPort = ownerQueryPort;
        this.employeeQueryPort = employeeQueryPort;
        this.branchQueryPort = branchQueryPort;
        this.companyQueryPort = companyQueryPort;
        this.confirmationEmailSender = confirmationEmailSender;
        this.appointmentMetrics = appointmentMetrics;
    }

    @Override
    @Transactional
    public AppointmentDto execute(CreateAppointmentCommand command) {
        // Issue #114: lock pesimista ACOTADO por empresa como PRIMERA sentencia,
        // antes de CUALQUIER lectura -incluida la resolución del empleado que sigue
        // dos líneas más abajo-. Serializa contra otra creación concurrente sobre el
        // mismo empleado: la segunda transacción espera aquí hasta que la primera
        // confirma o revierte, y entonces el findOverlapping de más abajo SÍ ve la
        // cita que la primera acaba de guardar. Cubre los solapes PARCIALES -horas de
        // inicio distintas que se pisan en el tiempo- que la constraint única
        // uq_appointments_active_employee_start (changeset 226) no alcanza, porque
        // esa solo atrapa el solape EXACTO de start_at. Ver el javadoc de
        // JpaEmployeeQueryPort.lockForOverlapCheck para el porqué de la granularidad
        // por empleado y no por (empleado, día).
        employeeQueryPort.lockForOverlapCheck(command.employeeId(), command.companyId());

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

        // Sede: si el request trae branchId debe pertenecer a la empresa y estar
        // ACTIVA; si no, la sede
        // activa por defecto ("Principal" activa / primera activa). No se agenda en una
        // sede fuera de
        // operación.
        BranchRef branch = command.branchId() != null
                ? resolveRequestedBranch(command.branchId(), command.companyId())
                : branchQueryPort.findDefaultActiveByCompanyId(command.companyId())
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Company has no active branch: " + command.companyId()));

        Appointment appointment = Appointment.create(command.startAt(), command.durationMinutes(),
                command.type(), command.notes(), animal, owner, command.clientName(),
                command.clientPhone(), command.clientEmail(), employee,
                CompanyRef.of(command.companyId()), branch);

        // BE-17: el cruce de horarios se comprueba ANTES de guardar y ANTES de
        // resolver el correo. Hacerlo después dejaba dos citas encima y —peor— el
        // sendAfterCommit ya encolado: salía la confirmación de una cita rechazada.
        int defaultMinutes = durationPolicyPort.defaultDurationMinutes(command.companyId());
        LocalDateTime endAt = appointment.endAt(defaultMinutes);
        List<Overlap> overlaps = repository.findOverlapping(command.companyId(),
                command.employeeId(), appointment.getStartAt(), endAt, defaultMinutes, null);
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
                    "Appointment overlap forced on create: employeeId={} startAt={} count={}"
                            + " overlappingIds={}",
                    command.employeeId(), appointment.getStartAt(), overlaps.size(),
                    AppointmentOverlaps.allIds(overlaps));
        }

        Appointment saved = repository.save(appointment);

        // Notificación al cliente (async y no bloqueante; si falla, el agendamiento
        // sigue). Los datos se resuelven AQUÍ, con la transacción y su conexión
        // abiertas; el envío se difiere al commit: mientras la transacción pueda
        // revertirse, la cita no existe y el correo no debe salir.
        sendAfterCommit(
                buildConfirmationData(saved, employee, owner, animal, branch, command.companyId()));

        appointmentMetrics.transitioned(saved.getStatus(), Channel.STAFF);
        // Solo puede venir no vacío si se forzó: el bloqueo ya lanzó en caso
        // contrario. Y solo con las citas visibles para el caller.
        return AppointmentDto.from(saved, visibleOverlapIds);
    }

    /**
     * Resuelve los datos del correo de confirmación del cliente. Destinatario:
     *
     * <ul>
     * <li>propietario registrado → su correo (si tiene), nombre = nombre del
     * propietario;
     * <li>contacto libre → el {@code clientEmail} opcional de la cita, nombre =
     * clientName.
     * </ul>
     *
     * Devuelve {@code null} si no hay a quién escribirle (propietario sin email o
     * contacto libre sin email). Las consultas viven aquí y no en el callback de
     * {@link #sendAfterCommit} a propósito: después del commit la conexión ya
     * volvió al pool y cada una abriría la suya, añadiendo latencia a la respuesta.
     */
    private AppointmentConfirmationData buildConfirmationData(Appointment saved,
            EmployeeRef employee, OwnerRef owner, AnimalRef animal, BranchRef branch,
            Long companyId) {
        String recipientEmail;
        String recipientName;
        if (owner != null) {
            recipientEmail = ownerQueryPort.findEmailByIdAndCompanyId(owner.id(), companyId)
                    .filter(e -> !e.isBlank()).orElse(null);
            recipientName = owner.name();
        } else {
            recipientEmail = saved.getClientEmail(); // ya normalizado (blank → null) en el dominio
            recipientName = saved.getClientName();
        }
        if (recipientEmail == null || recipientEmail.isBlank())
            return null;

        String companyName = companyQueryPort.findNameById(companyId).orElse(null);
        String branchAddress = branchQueryPort.findAddressById(branch.id()).orElse(null);
        String petName = animal != null ? animal.name() : null;
        return new AppointmentConfirmationData(recipientEmail, recipientName, companyName,
                saved.getStartAt(), saved.getType(), employee.name(), petName, branch.name(),
                branchAddress, saved.getNotes());
    }

    /**
     * Difiere el envío al commit. El adaptador es {@code @Async}, así que encolar
     * dentro de la transacción entregaba el correo sin esperar al desenlace: un
     * rollback posterior —p. ej. el {@code ObjectOptimisticLockingFailureException}
     * de otra entidad— dejaba al cliente con la confirmación de una cita que no
     * existe (BE-18).
     *
     * <p>
     * Sin transacción activa —un test unitario, o un caller futuro sin
     * {@code @Transactional}— se envía en el acto, que es el comportamiento
     * correcto ahí; registrar la sincronización lanzaría
     * {@code IllegalStateException}.
     *
     * <p>
     * El port no debe lanzar por contrato, pero el {@code catch} protege igualmente
     * al caller: una excepción en {@code afterCommit} se propaga aunque la
     * transacción ya haya confirmado, y convertiría una cita agendada en un 500.
     */
    private void sendAfterCommit(AppointmentConfirmationData confirmation) {
        if (confirmation == null) {
            return;
        }
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            confirmationEmailSender.send(confirmation);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    confirmationEmailSender.send(confirmation);
                } catch (RuntimeException exception) {
                    log.warn("No se pudo enviar la confirmación de la cita agendada: {}",
                            exception.getMessage());
                }
            }
        });
    }

    // Sede solicitada explícitamente: activa y de la empresa. Distingue "inactiva"
    // de "inexistente"
    // para dar
    // un error preciso (la sede existe pero fue desactivada vs. no pertenece a la
    // empresa / no
    // existe).
    private BranchRef resolveRequestedBranch(Long branchId, Long companyId) {
        return branchQueryPort.findActiveByIdAndCompanyId(branchId, companyId)
                .orElseThrow(() -> branchQueryPort.existsByIdAndCompanyId(branchId, companyId)
                        ? new IllegalArgumentException("Branch is not active: " + branchId)
                        : new IllegalArgumentException("Branch not found: " + branchId));
    }
}
