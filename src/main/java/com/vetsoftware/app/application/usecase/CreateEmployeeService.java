package com.vetsoftware.app.application.usecase;

import com.vetsoftware.app.application.command.CreateEmployeeCommand;
import com.vetsoftware.app.application.dto.EmployeeDto;
import com.vetsoftware.app.application.port.in.CreateEmployeeUseCase;
import com.vetsoftware.app.application.port.out.EmployeeRepository;
import com.vetsoftware.app.domain.Employee;
import com.vetsoftware.app.domain.EmployeeStatus;
import org.springframework.stereotype.Service;

@Service
public class CreateEmployeeService implements CreateEmployeeUseCase {
    private final EmployeeRepository repository;

    public CreateEmployeeService(EmployeeRepository repository) {
        this.repository = repository;
    }

    @Override
    public EmployeeDto execute(CreateEmployeeCommand command) {
        EmployeeStatus status = EmployeeStatus.valueOf(command.status().toUpperCase());
        Employee employee = Employee.create(
            command.employeeCode(), command.password(), command.name(), command.email(),
            status, command.companyId(), command.createdBy()
        );
        return EmployeeDto.from(repository.save(employee));
    }
}
