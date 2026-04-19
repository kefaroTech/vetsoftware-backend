package com.vetsoftware.app.application.usecase;

import com.vetsoftware.app.application.dto.EmployeeDto;
import com.vetsoftware.app.application.port.in.FindEmployeeUseCase;
import com.vetsoftware.app.application.port.out.EmployeeRepository;
import com.vetsoftware.app.domain.EmployeeNotFoundException;
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
