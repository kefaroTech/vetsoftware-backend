package com.vetsoftware.app.employee.application.usecase;

import com.vetsoftware.app.employee.application.command.CreateEmployeeCommand;
import com.vetsoftware.app.employee.application.dto.EmployeeDto;
import com.vetsoftware.app.employee.application.port.in.CreateEmployeeUseCase;
import com.vetsoftware.app.employee.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.employee.application.port.out.EmployeeRepository;
import com.vetsoftware.app.employee.domain.CompanyRef;
import com.vetsoftware.app.employee.domain.Employee;
import com.vetsoftware.app.employee.domain.EmployeeStatus;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "employee.create")
@Service
public class CreateEmployeeService implements CreateEmployeeUseCase {
    private final EmployeeRepository repository;
    private final CompanyQueryPort companyQueryPort;

    public CreateEmployeeService(EmployeeRepository repository, CompanyQueryPort companyQueryPort) {
        this.repository = repository;
        this.companyQueryPort = companyQueryPort;
    }

    @Override
    public EmployeeDto execute(CreateEmployeeCommand command) {
        EmployeeStatus status = EmployeeStatus.valueOf(command.status().toUpperCase());
        CompanyRef company = companyQueryPort.findById(command.companyId())
            .orElseThrow(() -> new IllegalArgumentException("Company not found: " + command.companyId()));
        Employee employee = Employee.create(
            command.employeeCode(), command.password(), command.name(), command.email(),
            status, company
        );
        return EmployeeDto.from(repository.save(employee));
    }
}
