package com.vetsoftware.app.auth.infrastructure.security;

import com.vetsoftware.app.auth.application.dto.EmployeeContext;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component("authz")
public class Authz {

    public boolean isMyCompany(Long companyId) {
        if (companyId == null) return false;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null
            && auth.getPrincipal() instanceof EmployeeContext me
            && companyId.equals(me.companyId());
    }

    public Long currentCompanyId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof EmployeeContext me) {
            return me.companyId();
        }
        throw new AccessDeniedException("No employee context");
    }
}
