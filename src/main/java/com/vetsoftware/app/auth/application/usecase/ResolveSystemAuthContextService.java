package com.vetsoftware.app.auth.application.usecase;

import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.auth.application.dto.SystemUserContext;
import com.vetsoftware.app.auth.application.exception.SessionReplacedException;
import com.vetsoftware.app.auth.application.port.in.ResolveSystemAuthContextUseCase;
import com.vetsoftware.app.auth.application.port.out.AuthSystemUserRepository;
import com.vetsoftware.app.auth.application.port.out.SystemPermissionResolver;
import java.util.Objects;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "auth.resolveSystemContext")
@Service
public class ResolveSystemAuthContextService implements ResolveSystemAuthContextUseCase {

    private final SystemPermissionResolver permissionResolver;
    private final AuthSystemUserRepository systemUserRepository;

    public ResolveSystemAuthContextService(SystemPermissionResolver permissionResolver,
                                           AuthSystemUserRepository systemUserRepository) {
        this.permissionResolver = permissionResolver;
        this.systemUserRepository = systemUserRepository;
    }

    @Override
    public AuthContext execute(Long systemUserId, Long authVersion) {
        if (systemUserId == null) return null;
        var systemUser = systemUserRepository.findActiveById(systemUserId).orElse(null);
        if (systemUser == null) return null;
        if (!Objects.equals(systemUser.authVersion(), authVersion)) {
            throw new SessionReplacedException();
        }
        return new SystemUserContext(systemUserId, permissionResolver.resolveFor(systemUserId));
    }
}
