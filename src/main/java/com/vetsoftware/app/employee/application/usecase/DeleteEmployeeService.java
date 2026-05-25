package com.vetsoftware.app.employee.application.usecase;

import com.vetsoftware.app.employee.application.port.in.DeleteEmployeeUseCase;
import com.vetsoftware.app.employee.application.port.out.EmployeeRepository;
import com.vetsoftware.app.employee.application.port.out.EmployeeRoleChildrenQueryPort;
import com.vetsoftware.app.employee.application.port.out.EmployeeRolesQueryPort;
import com.vetsoftware.app.employee.domain.AdminEmployeeCannotBeDisabledException;
import com.vetsoftware.app.employee.domain.EmployeeHasActiveChildrenException;
import com.vetsoftware.app.employee.domain.EmployeeNotFoundException;
import com.vetsoftware.app.employee.domain.RoleSnapshot;
import io.micrometer.observation.annotation.Observed;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "employee.delete")
@Service
public class DeleteEmployeeService implements DeleteEmployeeUseCase {
    private static final String ADMIN_ROLE_CODE = "ADMIN";

    private final EmployeeRepository repository;
    private final EmployeeRoleChildrenQueryPort employeeRoleChildrenQueryPort;
    private final EmployeeRolesQueryPort employeeRolesQueryPort;

    public DeleteEmployeeService(
            EmployeeRepository repository,
            EmployeeRoleChildrenQueryPort employeeRoleChildrenQueryPort,
            EmployeeRolesQueryPort employeeRolesQueryPort) {
        this.repository = repository;
        this.employeeRoleChildrenQueryPort = employeeRoleChildrenQueryPort;
        this.employeeRolesQueryPort = employeeRolesQueryPort;
    }

    @Override
    @Transactional
    public void execute(Long id) {
        repository.findById(id).orElseThrow(() -> new EmployeeNotFoundException(id));
        List<RoleSnapshot> roles = employeeRolesQueryPort
            .findRolesByEmployeeIds(List.of(id))
            .getOrDefault(id, List.of());
        if (roles.stream().anyMatch(r -> ADMIN_ROLE_CODE.equals(r.code()))) {
            throw new AdminEmployeeCannotBeDisabledException(id);
        }
        if (employeeRoleChildrenQueryPort.existsActiveByEmployeeId(id)) {
            throw new EmployeeHasActiveChildrenException(id, "employeeRole");
        }
        repository.delete(id);
    }
}
