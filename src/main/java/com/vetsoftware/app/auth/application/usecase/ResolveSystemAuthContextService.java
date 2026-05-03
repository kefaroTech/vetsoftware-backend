package com.vetsoftware.app.auth.application.usecase;

import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.auth.application.port.in.ResolveSystemAuthContextUseCase;
import com.vetsoftware.app.auth.application.port.out.SystemPermissionResolver;
import org.springframework.stereotype.Service;

@Service
public class ResolveSystemAuthContextService implements ResolveSystemAuthContextUseCase {

    private final SystemPermissionResolver permissionResolver;

    public ResolveSystemAuthContextService(SystemPermissionResolver permissionResolver) {
        this.permissionResolver = permissionResolver;
    }

    @Override
    public AuthContext execute(Long systemUserId) {
        return new AuthContext(systemUserId, null, permissionResolver.resolveFor(systemUserId));
    }
}
