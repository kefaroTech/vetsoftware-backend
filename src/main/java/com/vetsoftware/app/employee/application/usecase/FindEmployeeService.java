package com.vetsoftware.app.employee.application.usecase;

import com.vetsoftware.app.employee.application.dto.EmployeeDto;
import com.vetsoftware.app.employee.application.port.in.FindEmployeeUseCase;
import com.vetsoftware.app.employee.application.port.out.EmployeeRepository;
import com.vetsoftware.app.employee.domain.EmployeeNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "employee.find")
@Service
public class FindEmployeeService implements FindEmployeeUseCase {
    private final EmployeeRepository repository;

    public FindEmployeeService(EmployeeRepository repository) {
        this.repository = repository;
    }

    @Override
    public EmployeeDto findById(Long id, Long companyId) {
        return repository.findByIdAndCompanyId(id, companyId).map(EmployeeDto::from)
                .orElseThrow(() -> new EmployeeNotFoundException(id));
    }
}
