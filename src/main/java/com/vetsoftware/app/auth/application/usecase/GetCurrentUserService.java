package com.vetsoftware.app.auth.application.usecase;

import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.auth.application.dto.EmployeeContext;
import com.vetsoftware.app.auth.application.dto.MeDto;
import com.vetsoftware.app.auth.application.dto.SystemContext;
import com.vetsoftware.app.auth.application.dto.SystemUserContext;
import com.vetsoftware.app.auth.application.port.in.GetCurrentUserUseCase;
import com.vetsoftware.app.auth.application.port.out.EmployeeProfileQueryPort;
import com.vetsoftware.app.auth.application.port.out.EmployeeProfileQueryPort.EmployeeProfile;
import com.vetsoftware.app.auth.application.port.out.SystemUserProfileQueryPort;
import com.vetsoftware.app.auth.application.port.out.SystemUserProfileQueryPort.SystemUserProfile;
import io.micrometer.observation.annotation.Observed;
import java.util.Set;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Observed(name = "auth.current.user")
@Service
public class GetCurrentUserService implements GetCurrentUserUseCase {

    private static final String NOT_A_USER_CONTEXT = "Not an authenticated user context";

    private final EmployeeProfileQueryPort employeeProfileQueryPort;
    private final SystemUserProfileQueryPort systemUserProfileQueryPort;

    public GetCurrentUserService(EmployeeProfileQueryPort employeeProfileQueryPort,
            SystemUserProfileQueryPort systemUserProfileQueryPort) {
        this.employeeProfileQueryPort = employeeProfileQueryPort;
        this.systemUserProfileQueryPort = systemUserProfileQueryPort;
    }

    @Override
    public MeDto execute() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        AuthContext principal = AuthContext.ofPrincipal(auth == null ? null : auth.getPrincipal());

        return switch (principal) {
            case EmployeeContext me -> {
                EmployeeProfile profile = employeeProfileQueryPort.findById(me.employeeId())
                        .orElseThrow(() -> new AccessDeniedException("Employee profile not found"));
                yield new MeDto(me.employeeId(), "EMPLOYEE", me.companyId(), profile.name(),
                        profile.employeeCode(), profile.mustChangePassword(), me.permissions(),
                        me.branchIds());
            }
            case SystemUserContext me -> {
                SystemUserProfile profile = systemUserProfileQueryPort.findById(me.systemUserId())
                        .orElseThrow(
                                () -> new AccessDeniedException("System user profile not found"));
                yield new MeDto(me.systemUserId(), "SYSTEM_USER", null, profile.code(), null, false,
                        me.permissions(), Set.of());
            }
            case SystemContext _ -> throw new AccessDeniedException(NOT_A_USER_CONTEXT);
            case null -> throw new AccessDeniedException(NOT_A_USER_CONTEXT);
        };
    }
}
