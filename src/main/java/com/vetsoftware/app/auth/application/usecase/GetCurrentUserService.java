package com.vetsoftware.app.auth.application.usecase;

import com.vetsoftware.app.auth.application.dto.EmployeeContext;
import com.vetsoftware.app.auth.application.dto.MeDto;
import com.vetsoftware.app.auth.application.port.in.GetCurrentUserUseCase;
import com.vetsoftware.app.auth.application.port.out.EmployeeProfileQueryPort;
import com.vetsoftware.app.auth.application.port.out.EmployeeProfileQueryPort.EmployeeProfile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class GetCurrentUserService implements GetCurrentUserUseCase {

    private final EmployeeProfileQueryPort employeeProfileQueryPort;

    public GetCurrentUserService(EmployeeProfileQueryPort employeeProfileQueryPort) {
        this.employeeProfileQueryPort = employeeProfileQueryPort;
    }

    @Override
    public MeDto execute() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof EmployeeContext me)) {
            throw new AccessDeniedException("Not an employee context");
        }
        EmployeeProfile profile = employeeProfileQueryPort.findById(me.employeeId())
                .orElseThrow(() -> new AccessDeniedException("Employee profile not found"));
        return new MeDto(
                me.employeeId(),
                "EMPLOYEE",
                me.companyId(),
                profile.name(),
                profile.employeeCode(),
                me.permissions()
        );
    }
}
