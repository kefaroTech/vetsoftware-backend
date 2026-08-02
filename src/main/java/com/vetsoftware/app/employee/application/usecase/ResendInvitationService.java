package com.vetsoftware.app.employee.application.usecase;

import com.vetsoftware.app.employee.application.command.ResendInvitationCommand;
import com.vetsoftware.app.employee.application.dto.EmployeeDto;
import com.vetsoftware.app.employee.application.port.in.ResendInvitationUseCase;
import com.vetsoftware.app.employee.application.port.out.EmployeeInvitationEmailSender;
import com.vetsoftware.app.employee.application.port.out.EmployeeRepository;
import com.vetsoftware.app.employee.application.port.out.EmployeeRolesQueryPort;
import com.vetsoftware.app.employee.domain.Employee;
import com.vetsoftware.app.employee.domain.EmployeeNotFoundException;
import com.vetsoftware.app.employee.domain.EmployeeStatus;
import com.vetsoftware.app.employee.domain.RoleSnapshot;
import com.vetsoftware.app.infrastructure.security.PasswordHasher;
import io.micrometer.observation.annotation.Observed;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reenvía la invitación a un empleado en estado INVITED: le asigna una nueva contraseña provisional
 * (la anterior ya está ofuscada en hash, por eso el admin escribe una nueva), vuelve a exigir el
 * cambio en el primer login y reenvía el correo con sus datos de acceso. El correo es
 * async/best-effort (no bloquea).
 */
@Observed(name = "employee.resend.invitation")
@Service
public class ResendInvitationService implements ResendInvitationUseCase {

  private final EmployeeRepository repository;
  private final EmployeeRolesQueryPort rolesQueryPort;
  private final PasswordHasher passwordHasher;
  private final EmployeeInvitationEmailSender invitationEmailSender;

  public ResendInvitationService(
      EmployeeRepository repository,
      EmployeeRolesQueryPort rolesQueryPort,
      PasswordHasher passwordHasher,
      EmployeeInvitationEmailSender invitationEmailSender) {
    this.repository = repository;
    this.rolesQueryPort = rolesQueryPort;
    this.passwordHasher = passwordHasher;
    this.invitationEmailSender = invitationEmailSender;
  }

  @Override
  @Transactional
  public EmployeeDto execute(ResendInvitationCommand command) {
    Employee employee =
        repository
            .findById(command.employeeId())
            .orElseThrow(() -> new EmployeeNotFoundException(command.employeeId()));

    // El empleado debe pertenecer a la company del solicitante (cross-tenant → 404 sin filtrar
    // datos).
    if (!employee.getCompany().id().equals(command.companyId()))
      throw new EmployeeNotFoundException(command.employeeId());

    // Solo se reenvía a quien sigue invitado (nunca inició sesión). Un empleado ya activo no se
    // re-invita.
    if (employee.getStatus() != EmployeeStatus.INVITED)
      throw new IllegalArgumentException(
          "Solo se puede reenviar la invitación a empleados en estado invitado");

    employee.reinvite(passwordHasher.hash(command.password()));
    Employee saved = repository.save(employee);

    List<RoleSnapshot> roles =
        rolesQueryPort
            .findRolesByEmployeeIds(List.of(saved.getId()))
            .getOrDefault(saved.getId(), List.of());
    String roleNames = String.join(", ", roles.stream().map(RoleSnapshot::name).toList());

    invitationEmailSender.send(
        saved.getEmail(),
        saved.getName(),
        saved.getCompany().name(),
        saved.getEmployeeCode(),
        command.password(),
        roleNames);

    return EmployeeDto.from(saved, roles);
  }
}
