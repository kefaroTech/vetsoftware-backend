package com.vetsoftware.app.application.usecase;

import com.vetsoftware.app.application.port.in.DeleteEmployeeUseCase;
import com.vetsoftware.app.application.port.out.EmployeeRepository;
import com.vetsoftware.app.domain.EmployeeNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeleteEmployeeService implements DeleteEmployeeUseCase {
    private final EmployeeRepository repository;

    public DeleteEmployeeService(EmployeeRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void execute(Long id) {
        repository.findById(id).orElseThrow(() -> new EmployeeNotFoundException(id));
        repository.delete(id);
    }
}
