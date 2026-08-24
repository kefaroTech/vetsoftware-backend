package com.vetsoftware.app.auth.application.usecase;

import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.auth.application.dto.EmployeeContext;
import com.vetsoftware.app.auth.application.exception.SessionReplacedException;
import com.vetsoftware.app.auth.application.port.in.ResolveAuthContextUseCase;
import com.vetsoftware.app.auth.application.port.out.AuthEmployeeRepository;
import com.vetsoftware.app.auth.application.port.out.BranchAccessResolver;
import com.vetsoftware.app.auth.application.port.out.EffectivePermissionResolver;
import com.vetsoftware.app.auth.application.port.out.PermissionResolver;
import io.micrometer.observation.annotation.Observed;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Observed(name = "auth.resolve.context")
@Service
public class ResolveAuthContextService implements ResolveAuthContextUseCase {

    private final PermissionResolver permissionResolver;
    private final EffectivePermissionResolver effectivePermissionResolver;
    private final BranchAccessResolver branchAccessResolver;
    private final AuthEmployeeRepository employeeRepository;

    public ResolveAuthContextService(PermissionResolver permissionResolver,
            EffectivePermissionResolver effectivePermissionResolver,
            BranchAccessResolver branchAccessResolver, AuthEmployeeRepository employeeRepository) {
        this.permissionResolver = permissionResolver;
        this.effectivePermissionResolver = effectivePermissionResolver;
        this.branchAccessResolver = branchAccessResolver;
        this.employeeRepository = employeeRepository;
    }

    @Override
    public AuthContext execute(Long employeeId, Long authVersion) {
        if (employeeId == null)
            return null;
        var employee = employeeRepository.findActiveById(employeeId).orElse(null);
        if (employee == null)
            return null;
        if (!Objects.equals(employee.authVersion(), authVersion)) {
            throw new SessionReplacedException();
        }
        var basePermissions = permissionResolver.resolveFor(employee.id());
        return new EmployeeContext(employee.id(), employee.companyId(),
                effectivePermissionResolver.resolveFor(employee.companyId(), basePermissions),
                branchAccessResolver.resolveFor(employee.id()));
    }
}
