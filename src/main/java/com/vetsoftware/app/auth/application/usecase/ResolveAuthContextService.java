package com.vetsoftware.app.auth.application.usecase;

import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.auth.application.dto.EmployeeContext;
import com.vetsoftware.app.auth.application.port.in.ResolveAuthContextUseCase;
import com.vetsoftware.app.auth.application.port.out.AuthEmployeeRepository;
import com.vetsoftware.app.auth.application.port.out.PermissionResolver;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class ResolveAuthContextService implements ResolveAuthContextUseCase {

    private final PermissionResolver permissionResolver;
    private final AuthEmployeeRepository employeeRepository;

    public ResolveAuthContextService(PermissionResolver permissionResolver,
                                     AuthEmployeeRepository employeeRepository) {
        this.permissionResolver = permissionResolver;
        this.employeeRepository = employeeRepository;
    }

    @Override
    public AuthContext execute(Long employeeId, Long authVersion) {
        if (employeeId == null || authVersion == null) return null;
        return employeeRepository.findActiveById(employeeId)
            .filter(employee -> Objects.equals(employee.authVersion(), authVersion))
            .map(employee -> new EmployeeContext(
                employee.id(),
                employee.companyId(),
                permissionResolver.resolveFor(employee.id())))
            .orElse(null);
    }
}
