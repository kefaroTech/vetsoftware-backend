package com.vetsoftware.app.employee.application.usecase;

import com.vetsoftware.app.employee.application.port.in.DeleteEmployeeUseCase;
import com.vetsoftware.app.employee.application.port.out.EmployeeRepository;
import com.vetsoftware.app.employee.application.port.out.EmployeeRoleChildrenQueryPort;
import com.vetsoftware.app.employee.domain.EmployeeHasActiveChildrenException;
import com.vetsoftware.app.employee.domain.EmployeeNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "employee.delete")
@Service
public class DeleteEmployeeService implements DeleteEmployeeUseCase {
    private final EmployeeRepository repository;
    private final EmployeeRoleChildrenQueryPort employeeRoleChildrenQueryPort;

    public DeleteEmployeeService(
            EmployeeRepository repository,
            EmployeeRoleChildrenQueryPort employeeRoleChildrenQueryPort) {
        this.repository = repository;
        this.employeeRoleChildrenQueryPort = employeeRoleChildrenQueryPort;
    }

    @Override
    @Transactional
    public void execute(Long id) {
        repository.findById(id).orElseThrow(() -> new EmployeeNotFoundException(id));
        if (employeeRoleChildrenQueryPort.existsActiveByEmployeeId(id)) {
            throw new EmployeeHasActiveChildrenException(id, "employeeRole");
        }
        repository.delete(id);
    }
}
