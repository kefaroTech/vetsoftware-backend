package com.vetsoftware.app.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vetsoftware.app.auth.application.dto.EmployeeContext;
import com.vetsoftware.app.auth.application.dto.SystemContext;
import com.vetsoftware.app.auth.application.dto.SystemUserContext;
import com.vetsoftware.app.auth.infrastructure.filter.AuthFilter;
import com.vetsoftware.app.auth.infrastructure.security.Authz;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

class SystemRoleIsolationTest {

    @AfterEach
    void clearContexts() {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void onlySystemPrincipalsReceiveTheSystemRole() {
        var employee = new EmployeeContext(7L, 3L, Set.of("company.update"), Set.of(10L));
        var systemUser = new SystemUserContext(1L, Set.of());

        Authentication employeeAuth = authenticationFor(employee);
        Authentication systemUserAuth = authenticationFor(systemUser);
        Authentication internalSystemAuth = authenticationFor(SystemContext.INSTANCE);

        assertFalse(hasSystemRole(employeeAuth));
        assertTrue(hasSystemRole(systemUserAuth));
        assertTrue(hasSystemRole(internalSystemAuth));
    }

    @Test
    void systemUserIsSuperadminWithoutAnyWildcardPermission() {
        var authz = new Authz();
        var employee = new EmployeeContext(7L, 3L, Set.of("company.update"), Set.of(10L));
        authenticate(employee);

        assertFalse(authz.isSuperAdmin());
        assertTrue(authz.canAccessBranch(10L));
        assertFalse(authz.canAccessBranch(99L));

        authenticate(new SystemUserContext(1L, Set.of()));
        assertTrue(authz.isSuperAdmin());
    }

    @Test
    void employeeUsesOwnCompanyAndSystemUserMustSendExplicitTenantHeader() {
        var authz = new Authz();
        authenticate(new EmployeeContext(7L, 3L, Set.of("company.read"), Set.of(10L)));
        assertEquals(3L, authz.currentCompanyId());

        authenticate(new SystemUserContext(1L, Set.of()));
        assertThrows(IllegalArgumentException.class, authz::currentCompanyId);

        var request = new MockHttpServletRequest();
        request.addHeader(Authz.COMPANY_SCOPE_HEADER, "9");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        assertEquals(9L, authz.currentCompanyId());
    }

    private static Authentication authenticationFor(Object principal) {
        return ReflectionTestUtils.invokeMethod(AuthFilter.class, "toAuthentication", principal);
    }

    private static boolean hasSystemRole(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_SYSTEM".equals(authority.getAuthority()));
    }

    private static void authenticate(Object principal) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of()));
    }
}
