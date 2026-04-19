package com.vetsoftware.app.application.usecase;

import com.vetsoftware.app.application.command.UpdateEmployeeCommand;
import com.vetsoftware.app.application.dto.EmployeeDto;
import com.vetsoftware.app.application.port.in.UpdateEmployeeUseCase;
import com.vetsoftware.app.application.port.out.EmployeeRepository;
import com.vetsoftware.app.domain.Employee;
import com.vetsoftware.app.domain.EmployeeNotFoundException;
import com.vetsoftware.app.domain.EmployeeStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UpdateEmployeeService implements UpdateEmployeeUseCase {
    private final EmployeeRepository repository;

    public UpdateEmployeeService(EmployeeRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public EmployeeDto execute(UpdateEmployeeCommand command) {
        Employee employee = repository.findById(command.id())
            .orElseThrow(() -> new EmployeeNotFoundException(command.id()));
        EmployeeStatus status = EmployeeStatus.valueOf(command.status().toUpperCase());
        employee.update(command.employeeCode(), command.name(), command.email(), status);
        return EmployeeDto.from(repository.save(employee));
    }
}
