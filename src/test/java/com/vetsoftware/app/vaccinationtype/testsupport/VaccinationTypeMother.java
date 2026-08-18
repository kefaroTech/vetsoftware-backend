package com.vetsoftware.app.vaccinationtype.testsupport;

import com.vetsoftware.app.vaccinationtype.application.command.CreateVaccinationTypeCommand;
import com.vetsoftware.app.vaccinationtype.application.command.UpdateVaccinationTypeCommand;
import com.vetsoftware.app.vaccinationtype.domain.CompanyRef;
import com.vetsoftware.app.vaccinationtype.domain.VaccinationType;
import java.time.LocalDateTime;

/**
 * Fixtures del modulo vaccinationtype.
 *
 * <p>
 * Los tipos se construyen con el constructor publico y no con
 * {@code VaccinationType.create(...)}: el factory pone
 * {@code LocalDateTime.now()} y haria no deterministas las aserciones sobre
 * {@code createdDate}.
 */
public final class VaccinationTypeMother {

    public static final Long TYPE_ID = 50L;
    public static final Long COMPANY_ID = 9L;
    public static final Long OTRA_COMPANY_ID = 99L;

    public static final CompanyRef CLINICA = new CompanyRef(COMPANY_ID, "Clinica Norte", "NIT-900");
    public static final CompanyRef OTRA_CLINICA = new CompanyRef(OTRA_COMPANY_ID, "Clinica Sur",
            "NIT-990");

    public static final LocalDateTime CREADO = LocalDateTime.of(2026, 1, 15, 10, 30);

    private VaccinationTypeMother() {
    }

    /** Tipo propio de una empresa (no general), habilitado. El caso por defecto. */
    public static VaccinationType propia() {
        return propia(TYPE_ID);
    }

    public static VaccinationType propia(Long id) {
        return new VaccinationType(id, "Rabia", "Vacuna antirrabica", CLINICA, false, CREADO, true);
    }

    public static VaccinationType propia(Long id, CompanyRef empresa) {
        return new VaccinationType(id, "Rabia", "Vacuna antirrabica", empresa, false, CREADO, true);
    }

    public static VaccinationType deshabilitada() {
        return new VaccinationType(TYPE_ID, "Rabia", "Vacuna antirrabica", CLINICA, false, CREADO,
                false);
    }

    /** Tipo general, disponible para todas las empresas y sin compania. */
    public static VaccinationType general() {
        return new VaccinationType(TYPE_ID, "Rabia", "Vacuna antirrabica", null, true, CREADO,
                true);
    }

    public static CreateVaccinationTypeCommand comandoCrear() {
        return new CreateVaccinationTypeCommand("Rabia", "Vacuna antirrabica", COMPANY_ID, false);
    }

    public static UpdateVaccinationTypeCommand comandoActualizar() {
        return new UpdateVaccinationTypeCommand(TYPE_ID, "Moquillo", "Vacuna contra el moquillo",
                COMPANY_ID, false);
    }
}
