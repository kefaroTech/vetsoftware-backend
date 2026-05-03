package com.vetsoftware.app.auth.application.usecase;

import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.auth.application.port.in.ResolveAuthContextUseCase;
import com.vetsoftware.app.auth.application.port.out.PermissionResolver;
import org.springframework.stereotype.Service;

@Service
public class ResolveAuthContextService implements ResolveAuthContextUseCase {

    private final PermissionResolver permissionResolver;

    public ResolveAuthContextService(PermissionResolver permissionResolver) {
        this.permissionResolver = permissionResolver;
    }

    @Override
    public AuthContext execute(Long employeeId, Long companyId) {
        return new AuthContext(employeeId, companyId, permissionResolver.resolveFor(employeeId));
    }
}
