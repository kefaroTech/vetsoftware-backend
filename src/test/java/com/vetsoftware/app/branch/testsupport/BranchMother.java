package com.vetsoftware.app.branch.testsupport;

import com.vetsoftware.app.branch.domain.Branch;
import com.vetsoftware.app.branch.domain.CityRef;
import com.vetsoftware.app.branch.domain.CompanyRef;
import java.time.LocalDateTime;

/**
 * Object mother de {@code branch}: sucursales y sus companion VO (ciudad y
 * empresa) con valores válidos por defecto.
 */
public final class BranchMother {

    public static final Long BRANCH_ID = 1L;
    public static final Long CITY_ID = 5L;
    public static final Long COMPANY_ID = 9L;
    public static final LocalDateTime CREADO = LocalDateTime.of(2026, 1, 15, 8, 0);

    public static final CityRef BOGOTA = new CityRef(CITY_ID, "Bogotá");
    public static final CompanyRef CLINICA = new CompanyRef(COMPANY_ID, "Vet SAS", "900123456");

    private BranchMother() {
    }

    public static Branch sedeActiva() {
        return new Branch(BRANCH_ID, "Sede Norte", "NORTE", "Cra 1 #2-3", "3001112233", BOGOTA,
                CLINICA, CREADO, true);
    }

    public static Branch sedeInactiva() {
        return new Branch(BRANCH_ID, "Sede Norte", "NORTE", "Cra 1 #2-3", "3001112233", BOGOTA,
                CLINICA, CREADO, false);
    }

    public static Branch sinDireccionNiTelefono() {
        return new Branch(BRANCH_ID, "Sede Norte", "NORTE", null, null, BOGOTA, CLINICA, CREADO,
                true);
    }
}
