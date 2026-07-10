package com.vetsoftware.app.employee.application.usecase;

import com.vetsoftware.app.employee.application.command.ChangeMyPasswordCommand;
import com.vetsoftware.app.employee.application.port.in.ChangeMyPasswordUseCase;
import com.vetsoftware.app.employee.application.port.out.EmployeeRepository;
import com.vetsoftware.app.employee.domain.Employee;
import com.vetsoftware.app.employee.domain.EmployeeNotFoundException;
import com.vetsoftware.app.infrastructure.security.PasswordHasher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cambio de la propia contraseña (primer login forzado). Hashea la nueva contraseña, la asigna y limpia
 * {@code mustChangePassword}. No toca {@code authVersion}: la sesión en curso sigue válida y el empleado
 * pasa directo al panel sin re-login.
 */
@Service
public class ChangeMyPasswordService implements ChangeMyPasswordUseCase {

    private final EmployeeRepository repository;
    private final PasswordHasher passwordHasher;

    public ChangeMyPasswordService(EmployeeRepository repository, PasswordHasher passwordHasher) {
        this.repository = repository;
        this.passwordHasher = passwordHasher;
    }

    @Override
    @Transactional
    public boolean execute(ChangeMyPasswordCommand command) {
        Employee employee = repository.findById(command.employeeId())
            .orElseThrow(() -> new EmployeeNotFoundException(command.employeeId()));
        // Antes de cambiar: si estaba obligado a cambiarla, este cambio ES la aceptación de la invitación.
        boolean acceptedInvitation = employee.isMustChangePassword();
        employee.changePassword(passwordHasher.hash(command.newPassword()));
        repository.save(employee);
        return acceptedInvitation;
    }
}
