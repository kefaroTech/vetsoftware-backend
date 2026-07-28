package com.vetsoftware.app.employee.application.usecase;

import com.vetsoftware.app.employee.application.port.in.VerifyEmployeeEmailUseCase;
import com.vetsoftware.app.employee.application.port.out.EmployeeRepository;
import com.vetsoftware.app.employee.domain.Employee;
import com.vetsoftware.app.employee.domain.EmployeeNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "employee.verify.email")
@Service
public class VerifyEmployeeEmailService implements VerifyEmployeeEmailUseCase {

    private final EmployeeRepository repository;

    public VerifyEmployeeEmailService(EmployeeRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void execute(Long employeeId) {
        Employee employee = repository.findById(employeeId)
            .orElseThrow(() -> new EmployeeNotFoundException(employeeId));
        employee.verifyEmail();
        repository.save(employee);
    }
}
