package com.vetsoftware.app.employee.application.usecase;

import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.employee.application.port.in.DeleteEmployeeUseCase;
import com.vetsoftware.app.employee.application.port.out.EmployeeRepository;
import com.vetsoftware.app.employee.domain.EmployeeNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "employee.delete")
@Service
public class DeleteEmployeeService implements DeleteEmployeeUseCase {
    private final EmployeeRepository repository;

    public DeleteEmployeeService(EmployeeRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void execute(Long id, AuthContext auth) {
        repository.findById(id).orElseThrow(() -> new EmployeeNotFoundException(id));
        repository.delete(id);
    }
}
