package com.vetsoftware.app.employee.application.usecase;

import com.vetsoftware.app.employee.application.command.CreateEmployeeCommand;
import com.vetsoftware.app.employee.application.dto.EmployeeDto;
import com.vetsoftware.app.employee.application.port.in.CreateEmployeeUseCase;
import com.vetsoftware.app.employee.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.employee.application.port.out.EmployeeCapacityPort;
import com.vetsoftware.app.employee.application.port.out.EmployeePasswordHasherPort;
import com.vetsoftware.app.employee.application.port.out.EmployeeRepository;
import com.vetsoftware.app.employee.domain.CompanyRef;
import com.vetsoftware.app.employee.domain.Employee;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "employee.create")
@Service
public class CreateEmployeeService implements CreateEmployeeUseCase {
    private final EmployeeRepository repository;
    private final CompanyQueryPort companyQueryPort;
    private final EmployeeCapacityPort employeeCapacityPort;

    private final EmployeePasswordHasherPort passwordHasher;

    public CreateEmployeeService(EmployeeRepository repository, CompanyQueryPort companyQueryPort,
            EmployeePasswordHasherPort passwordHasher, EmployeeCapacityPort employeeCapacityPort) {
        this.repository = repository;
        this.companyQueryPort = companyQueryPort;
        this.passwordHasher = passwordHasher;
        this.employeeCapacityPort = employeeCapacityPort;
    }

    @Override
    @Transactional
    public EmployeeDto execute(CreateEmployeeCommand command) {
        CompanyRef company = companyQueryPort.findById(command.companyId()).orElseThrow(
                () -> new IllegalArgumentException("Company not found: " + command.companyId()));

        String hashed = passwordHasher.hash(command.password());

        // Este caso de uso lo usa el auto-registro del dueño (Opción B). No fuerza
        // cambio de contraseña
        // (el dueño elige la suya). El alta de staff por el admin va por
        // InviteEmployeeService.
        Employee employee = Employee.create(command.employeeCode(), hashed, command.name(),
                command.email(), company, command.emailVerified(), false);
        employeeCapacityPort.reserve(command.companyId());
        return EmployeeDto.from(repository.save(employee));
    }
}
