package com.vetsoftware.app.role.testsupport;

import com.vetsoftware.app.role.domain.CompanyRef;
import com.vetsoftware.app.role.domain.Role;
import java.time.LocalDateTime;

public final class RoleMother {

    public static final Long ROLE_ID = 1L;
    public static final Long COMPANY_ID = 9L;
    public static final Long OTRA_COMPANY_ID = 99L;
    public static final LocalDateTime CREADO = LocalDateTime.of(2026, 1, 15, 10, 30);

    public static final CompanyRef CLINICA_NORTE = new CompanyRef(COMPANY_ID, "Clinica Norte",
            "NIT-900");
    public static final CompanyRef CLINICA_SUR = new CompanyRef(OTRA_COMPANY_ID, "Clinica Sur",
            "NIT-901");

    private RoleMother() {
    }

    public static Role veterinario() {
        return new Role(ROLE_ID, "Veterinario", "VET", CLINICA_NORTE, CREADO, true);
    }

    public static Role administrador() {
        return new Role(2L, "Administrador", "ADMIN", CLINICA_NORTE, CREADO, true);
    }

    public static Role deshabilitado() {
        return new Role(ROLE_ID, "Veterinario", "VET", CLINICA_NORTE, CREADO, false);
    }
}
