package com.vetsoftware.app.employee.application.usecase;

import com.vetsoftware.app.employee.application.command.InviteEmployeeCommand;
import com.vetsoftware.app.employee.application.dto.EmployeeDto;
import com.vetsoftware.app.employee.application.port.in.InviteEmployeeUseCase;
import com.vetsoftware.app.employee.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.employee.application.port.out.EmployeeBranchAssigner;
import com.vetsoftware.app.employee.application.port.out.EmployeeInvitationEmailSender;
import com.vetsoftware.app.employee.application.port.out.EmployeeRepository;
import com.vetsoftware.app.employee.application.port.out.EmployeeRoleAssigner;
import com.vetsoftware.app.employee.domain.CompanyRef;
import com.vetsoftware.app.employee.domain.Employee;
import com.vetsoftware.app.infrastructure.security.PasswordHasher;
import io.micrometer.observation.annotation.Observed;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Alta de staff por un admin, en una sola transacción: crea el empleado
 * (verificado, con contraseña temporal y obligación de cambiarla en el primer
 * login), le asigna los roles y le envía la invitación por correo con sus datos
 * de acceso. El correo es async/best-effort (no bloquea ni revierte el alta).
 */
@Observed(name = "employee.invite")
@Service
public class InviteEmployeeService implements InviteEmployeeUseCase {

    private final EmployeeRepository repository;
    private final CompanyQueryPort companyQueryPort;
    private final PasswordHasher passwordHasher;
    private final EmployeeRoleAssigner roleAssigner;
    private final EmployeeBranchAssigner branchAssigner;
    private final EmployeeInvitationEmailSender invitationEmailSender;

    public InviteEmployeeService(EmployeeRepository repository, CompanyQueryPort companyQueryPort,
            PasswordHasher passwordHasher, EmployeeRoleAssigner roleAssigner,
            EmployeeBranchAssigner branchAssigner,
            EmployeeInvitationEmailSender invitationEmailSender) {
        this.repository = repository;
        this.companyQueryPort = companyQueryPort;
        this.passwordHasher = passwordHasher;
        this.roleAssigner = roleAssigner;
        this.branchAssigner = branchAssigner;
        this.invitationEmailSender = invitationEmailSender;
    }

    @Override
    @Transactional
    public EmployeeDto execute(InviteEmployeeCommand command) {
        // Regla: un empleado no puede crearse sin sede (quedaría bloqueado de
        // citas/cuentas/POS/reportes). Se valida
        // antes de tocar nada; el detalle (sedes ∈ empresa) lo revalida el assigner en
        // la misma
        // transacción.
        if (command.branchIds() == null || command.branchIds().isEmpty()) {
            throw new IllegalArgumentException("At least one branch is required");
        }

        CompanyRef company = companyQueryPort.findById(command.companyId()).orElseThrow(
                () -> new IllegalArgumentException("Company not found: " + command.companyId()));

        String hashed = passwordHasher.hash(command.password());
        // Staff invitado: verificado (lo crea el admin) y obligado a cambiar la
        // contraseña en el primer
        // login.
        Employee employee = Employee.create(command.employeeCode(), hashed, command.name(),
                command.email(), company, true, true);
        // IDENTITY → INSERT inmediato: si el código ya existe, falla aquí (antes de
        // asignar roles /
        // enviar correo).
        Employee saved = repository.save(employee);

        List<String> roleNames = new ArrayList<>();
        for (Long roleId : command.roleIds()) {
            roleNames.add(roleAssigner.assign(saved.getId(), roleId));
        }

        // Sedes en la misma transacción: si algo falla (sede inexistente/ajena),
        // revierte el alta
        // completa.
        branchAssigner.assign(saved.getId(), command.companyId(), command.branchIds());

        invitationEmailSender.send(saved.getEmail(), saved.getName(), company.name(),
                saved.getEmployeeCode(), command.password(), String.join(", ", roleNames));

        return EmployeeDto.from(saved);
    }
}
