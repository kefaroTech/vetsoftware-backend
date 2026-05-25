package com.vetsoftware.app.employee.application.usecase;

import com.vetsoftware.app.employee.application.command.UpdateEmployeeCommand;
import com.vetsoftware.app.employee.application.dto.EmployeeDto;
import com.vetsoftware.app.employee.application.port.in.UpdateEmployeeUseCase;
import com.vetsoftware.app.employee.application.port.out.EmployeeRepository;
import com.vetsoftware.app.employee.domain.Employee;
import com.vetsoftware.app.employee.domain.EmployeeNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "employee.update")
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
        employee.update(command.employeeCode(), command.name(), command.email());
        return EmployeeDto.from(repository.save(employee));
    }
}
