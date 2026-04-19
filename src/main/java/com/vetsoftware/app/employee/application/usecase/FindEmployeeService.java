package com.vetsoftware.app.employee.application.usecase;

import com.vetsoftware.app.employee.application.dto.EmployeeDto;
import com.vetsoftware.app.employee.application.port.in.FindEmployeeUseCase;
import com.vetsoftware.app.employee.application.port.out.EmployeeRepository;
import com.vetsoftware.app.employee.domain.EmployeeNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class FindEmployeeService implements FindEmployeeUseCase {
    private final EmployeeRepository repository;

    public FindEmployeeService(EmployeeRepository repository) {
        this.repository = repository;
    }

    @Override
    public EmployeeDto findById(Long id) {
        return repository.findById(id)
            .map(EmployeeDto::from)
            .orElseThrow(() -> new EmployeeNotFoundException(id));
    }
}
