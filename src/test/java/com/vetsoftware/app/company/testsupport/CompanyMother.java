package com.vetsoftware.app.company.testsupport;

import com.vetsoftware.app.company.application.command.CreateCompanyCommand;
import com.vetsoftware.app.company.application.command.UpdateCompanyCommand;
import com.vetsoftware.app.company.domain.CityRef;
import com.vetsoftware.app.company.domain.Company;
import com.vetsoftware.app.company.domain.MembershipRef;
import java.time.LocalDateTime;

/**
 * Fixtures del modulo company.
 *
 * <p>
 * Se construyen con el constructor publico y no con
 * {@code Company.create(...)}: el factory pone {@code LocalDateTime.now()} y
 * haria no deterministas las aserciones sobre {@code createdDate}.
 */
public final class CompanyMother {

    public static final Long COMPANY_ID = 9L;

    public static final CityRef BOGOTA = new CityRef(11L, "Bogota");
    public static final CityRef MEDELLIN = new CityRef(12L, "Medellin");

    public static final MembershipRef PREMIUM = new MembershipRef(21L, "Premium", "ACTIVE");
    public static final MembershipRef BASICA = new MembershipRef(22L, "Basica", "TRIAL");

    public static final LocalDateTime CREADO = LocalDateTime.of(2026, 1, 15, 10, 30);

    private CompanyMother() {
    }

    /** Empresa habilitada con todos los campos opcionales rellenos. */
    public static Company clinicaNorte() {
        return clinicaNorte(COMPANY_ID);
    }

    public static Company clinicaNorte(Long id) {
        return new Company(id, "Clinica Norte", "NIT-900", "Calle 123 #45-67", "3001234567", BOGOTA,
                PREMIUM, CREADO, true);
    }

    /** Misma empresa ya desactivada por el borrado logico. */
    public static Company deshabilitada() {
        return new Company(COMPANY_ID, "Clinica Norte", "NIT-900", "Calle 123 #45-67", "3001234567",
                BOGOTA, PREMIUM, CREADO, false);
    }

    /** Empresa sin direccion ni telefono: ambos campos son opcionales. */
    public static Company sinDatosOpcionales() {
        return new Company(COMPANY_ID, "Clinica Norte", "NIT-900", null, null, BOGOTA, PREMIUM,
                CREADO, true);
    }

    /** Comando de creacion coherente con las refs de arriba. */
    public static CreateCompanyCommand comandoCrear() {
        return new CreateCompanyCommand("Clinica Norte", "NIT-900", "Calle 123 #45-67",
                "3001234567", BOGOTA.id(), PREMIUM.id());
    }

    /** Comando de actualizacion que cambia todos los campos y las dos refs. */
    public static UpdateCompanyCommand comandoActualizar() {
        return new UpdateCompanyCommand(COMPANY_ID, "Clinica Sur", "NIT-901", "Carrera 45 #10-20",
                "3009876543", MEDELLIN.id(), BASICA.id());
    }
}
