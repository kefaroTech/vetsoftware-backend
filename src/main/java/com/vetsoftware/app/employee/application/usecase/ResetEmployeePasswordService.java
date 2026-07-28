package com.vetsoftware.app.employee.application.usecase;

import com.vetsoftware.app.employee.application.command.ResetEmployeePasswordCommand;
import com.vetsoftware.app.employee.application.port.in.ResetEmployeePasswordUseCase;
import com.vetsoftware.app.employee.application.port.out.EmployeeRepository;
import com.vetsoftware.app.employee.domain.Employee;
import com.vetsoftware.app.employee.domain.EmployeeNotFoundException;
import com.vetsoftware.app.infrastructure.security.PasswordHasher;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Restablece la contraseña del empleado: hashea la nueva, limpia mustChangePassword e invalida sesiones. */
@Observed(name = "employee.resetPassword")
@Service
public class ResetEmployeePasswordService implements ResetEmployeePasswordUseCase {

    private final EmployeeRepository repository;
    private final PasswordHasher passwordHasher;

    public ResetEmployeePasswordService(EmployeeRepository repository, PasswordHasher passwordHasher) {
        this.repository = repository;
        this.passwordHasher = passwordHasher;
    }

    @Override
    @Transactional
    public void execute(ResetEmployeePasswordCommand command) {
        Employee employee = repository.findByIdIncludingDisabled(command.employeeId())
            .orElseThrow(() -> new EmployeeNotFoundException(command.employeeId()));
        employee.resetPassword(passwordHasher.hash(command.newPassword()));
        repository.save(employee);
    }
}
