package com.vetsoftware.app.auth.testsupport;

import com.vetsoftware.app.auth.application.dto.EmployeeContext;
import com.vetsoftware.app.auth.application.dto.SystemUserContext;
import com.vetsoftware.app.auth.application.port.out.AuthEmployeeRepository.AuthEmployee;
import com.vetsoftware.app.auth.application.port.out.AuthSystemUserRepository.AuthSystemUser;
import com.vetsoftware.app.auth.application.port.out.EmployeeProfileQueryPort.EmployeeProfile;
import com.vetsoftware.app.auth.application.port.out.SystemUserProfileQueryPort.SystemUserProfile;
import java.util.Set;

/**
 * Fixtures de la feature auth: actores autenticados y las proyecciones que
 * devuelven sus puertos de salida.
 */
public final class AuthMother {

    public static final Long EMPLOYEE_ID = 7L;
    public static final Long COMPANY_ID = 3L;
    public static final Long SYSTEM_USER_ID = 2L;
    public static final Long AUTH_VERSION = 5L;
    public static final Long BRANCH_ID = 10L;
    public static final Long OTHER_BRANCH_ID = 99L;

    public static final Set<String> PERMISOS_EMPLEADO = Set.of("company.update", "animal.read");

    private AuthMother() {
    }

    /** Empleado autenticado con una sede asignada. El caso por defecto. */
    public static EmployeeContext empleado() {
        return new EmployeeContext(EMPLOYEE_ID, COMPANY_ID, PERMISOS_EMPLEADO, Set.of(BRANCH_ID));
    }

    public static EmployeeContext empleado(Set<String> permisos, Set<Long> sedes) {
        return new EmployeeContext(EMPLOYEE_ID, COMPANY_ID, permisos, sedes);
    }

    public static SystemUserContext usuarioDeSistema() {
        return new SystemUserContext(SYSTEM_USER_ID, Set.of());
    }

    public static AuthEmployee sesionEmpleado() {
        return new AuthEmployee(EMPLOYEE_ID, COMPANY_ID, AUTH_VERSION);
    }

    public static AuthSystemUser sesionSistema() {
        return new AuthSystemUser(SYSTEM_USER_ID, AUTH_VERSION);
    }

    public static EmployeeProfile perfilEmpleado() {
        return new EmployeeProfile(EMPLOYEE_ID, COMPANY_ID, "Ana Ruiz", "EMP-1", false);
    }

    public static SystemUserProfile perfilSistema() {
        return new SystemUserProfile(SYSTEM_USER_ID, "ADMIN");
    }
}
