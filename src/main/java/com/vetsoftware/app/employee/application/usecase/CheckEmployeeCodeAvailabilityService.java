package com.vetsoftware.app.employee.application.usecase;

import com.vetsoftware.app.employee.application.port.in.CheckEmployeeCodeAvailabilityUseCase;
import com.vetsoftware.app.employee.application.port.out.EmployeeRepository;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "employee.checkCodeAvailability")
@Service
public class CheckEmployeeCodeAvailabilityService implements CheckEmployeeCodeAvailabilityUseCase {

    private final EmployeeRepository repository;

    public CheckEmployeeCodeAvailabilityService(EmployeeRepository repository) {
        this.repository = repository;
    }

    @Override
    public boolean isAvailable(String employeeCode) {
        if (employeeCode == null) return false;
        String code = employeeCode.trim();
        if (code.isEmpty()) return false;
        return !repository.codeExists(code);
    }
}
