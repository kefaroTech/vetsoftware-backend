package com.vetsoftware.app.appointment.application.usecase;

import com.vetsoftware.app.appointment.application.command.CreateAppointmentCommand;
import com.vetsoftware.app.appointment.application.dto.AppointmentConfirmationData;
import com.vetsoftware.app.appointment.application.dto.AppointmentDto;
import com.vetsoftware.app.appointment.application.port.in.CreateAppointmentUseCase;
import com.vetsoftware.app.appointment.application.port.out.AnimalQueryPort;
import com.vetsoftware.app.appointment.application.port.out.AppointmentConfirmationEmailSender;
import com.vetsoftware.app.appointment.application.port.out.AppointmentMetrics;
import com.vetsoftware.app.appointment.application.port.out.AppointmentMetrics.Channel;
import com.vetsoftware.app.appointment.application.port.out.AppointmentRepository;
import com.vetsoftware.app.appointment.application.port.out.BranchQueryPort;
import com.vetsoftware.app.appointment.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.appointment.application.port.out.EmployeeQueryPort;
import com.vetsoftware.app.appointment.application.port.out.OwnerQueryPort;
import com.vetsoftware.app.appointment.domain.AnimalRef;
import com.vetsoftware.app.appointment.domain.Appointment;
import com.vetsoftware.app.appointment.domain.BranchRef;
import com.vetsoftware.app.appointment.domain.CompanyRef;
import com.vetsoftware.app.appointment.domain.EmployeeRef;
import com.vetsoftware.app.appointment.domain.OwnerRef;
import io.micrometer.observation.annotation.Observed;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "appointment.create")
@Service
public class CreateAppointmentService implements CreateAppointmentUseCase {
  private final AppointmentRepository repository;
  private final AnimalQueryPort animalQueryPort;
  private final OwnerQueryPort ownerQueryPort;
  private final EmployeeQueryPort employeeQueryPort;
  private final BranchQueryPort branchQueryPort;
  private final CompanyQueryPort companyQueryPort;
  private final AppointmentConfirmationEmailSender confirmationEmailSender;
  private final AppointmentMetrics appointmentMetrics;

  public CreateAppointmentService(
      AppointmentRepository repository,
      AnimalQueryPort animalQueryPort,
      OwnerQueryPort ownerQueryPort,
      EmployeeQueryPort employeeQueryPort,
      BranchQueryPort branchQueryPort,
      CompanyQueryPort companyQueryPort,
      AppointmentConfirmationEmailSender confirmationEmailSender,
      AppointmentMetrics appointmentMetrics) {
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
    EmployeeRef employee =
        employeeQueryPort
            .findByIdAndCompanyId(command.employeeId(), command.companyId())
            .orElseThrow(
                () -> new IllegalArgumentException("Employee not found: " + command.employeeId()));
    AnimalRef animal =
        command.animalId() == null
            ? null
            : animalQueryPort
                .findByIdAndCompanyId(command.animalId(), command.companyId())
                .orElseThrow(
                    () -> new IllegalArgumentException("Animal not found: " + command.animalId()));
    OwnerRef owner =
        command.ownerId() == null
            ? null
            : ownerQueryPort
                .findByIdAndCompanyId(command.ownerId(), command.companyId())
                .orElseThrow(
                    () -> new IllegalArgumentException("Owner not found: " + command.ownerId()));

    // Sede: si el request trae branchId debe pertenecer a la empresa y estar ACTIVA; si no, la sede
    // activa por defecto ("Principal" activa / primera activa). No se agenda en una sede fuera de
    // operación.
    BranchRef branch =
        command.branchId() != null
            ? resolveRequestedBranch(command.branchId(), command.companyId())
            : branchQueryPort
                .findDefaultActiveByCompanyId(command.companyId())
                .orElseThrow(
                    () ->
                        new IllegalArgumentException(
                            "Company has no active branch: " + command.companyId()));

    Appointment appointment =
        Appointment.create(
            command.startAt(),
            command.type(),
            command.notes(),
            animal,
            owner,
            command.clientName(),
            command.clientPhone(),
            command.clientEmail(),
            employee,
            CompanyRef.of(command.companyId()),
            branch);
    Appointment saved = repository.save(appointment);

    // Notificación al cliente (async y no bloqueante; si falla, el agendamiento sigue).
    sendConfirmationEmail(saved, employee, owner, animal, branch, command.companyId());

    List<Long> clashes =
        repository.findClashingIds(
            command.companyId(), command.employeeId(), command.startAt(), saved.getId());
    appointmentMetrics.transitioned(saved.getStatus(), Channel.STAFF);
    return AppointmentDto.from(saved, clashes);
  }

  /**
   * Envía el correo de confirmación al cliente. Destinatario:
   *
   * <ul>
   *   <li>propietario registrado → su correo (si tiene), nombre = nombre del propietario;
   *   <li>contacto libre → el {@code clientEmail} opcional de la cita, nombre = clientName.
   * </ul>
   *
   * Si no hay correo (propietario sin email o contacto libre sin email), no se envía nada. El envío
   * es {@code @Async} en el adaptador y nunca lanza.
   */
  private void sendConfirmationEmail(
      Appointment saved,
      EmployeeRef employee,
      OwnerRef owner,
      AnimalRef animal,
      BranchRef branch,
      Long companyId) {
    String recipientEmail;
    String recipientName;
    if (owner != null) {
      recipientEmail =
          ownerQueryPort
              .findEmailByIdAndCompanyId(owner.id(), companyId)
              .filter(e -> !e.isBlank())
              .orElse(null);
      recipientName = owner.name();
    } else {
      recipientEmail = saved.getClientEmail(); // ya normalizado (blank → null) en el dominio
      recipientName = saved.getClientName();
    }
    if (recipientEmail == null || recipientEmail.isBlank()) return;

    String companyName = companyQueryPort.findNameById(companyId).orElse(null);
    String branchAddress = branchQueryPort.findAddressById(branch.id()).orElse(null);
    String petName = animal != null ? animal.name() : null;
    confirmationEmailSender.send(
        new AppointmentConfirmationData(
            recipientEmail,
            recipientName,
            companyName,
            saved.getStartAt(),
            saved.getType(),
            employee.name(),
            petName,
            branch.name(),
            branchAddress,
            saved.getNotes()));
  }

  // Sede solicitada explícitamente: activa y de la empresa. Distingue "inactiva" de "inexistente"
  // para dar
  // un error preciso (la sede existe pero fue desactivada vs. no pertenece a la empresa / no
  // existe).
  private BranchRef resolveRequestedBranch(Long branchId, Long companyId) {
    return branchQueryPort
        .findActiveByIdAndCompanyId(branchId, companyId)
        .orElseThrow(
            () ->
                branchQueryPort.existsByIdAndCompanyId(branchId, companyId)
                    ? new IllegalArgumentException("Branch is not active: " + branchId)
                    : new IllegalArgumentException("Branch not found: " + branchId));
  }
}
