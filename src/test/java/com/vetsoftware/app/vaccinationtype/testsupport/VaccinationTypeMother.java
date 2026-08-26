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
    /** Id de la fila del catalogo de PLATAFORMA (sin empresa, general). */
    public static final Long GENERAL_TYPE_ID = 51L;
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
        return new VaccinationType(id, "Rabia", "Vacuna antirrabica", CLINICA, false, CREADO, null,
                true);
    }

    public static VaccinationType propia(Long id, CompanyRef empresa) {
        return new VaccinationType(id, "Rabia", "Vacuna antirrabica", empresa, false, CREADO, null,
                true);
    }

    public static VaccinationType deshabilitada() {
        return new VaccinationType(TYPE_ID, "Rabia", "Vacuna antirrabica", CLINICA, false, CREADO,
                null, false);
    }

    /** Tipo general, disponible para todas las empresas y sin compania. */
    public static VaccinationType general() {
        return new VaccinationType(TYPE_ID, "Rabia", "Vacuna antirrabica", null, true, CREADO, null,
                true);
    }

    /**
     * Fila del catalogo de PLATAFORMA dada de baja. Es la ocupante del nombre en la
     * rama de reactivacion global: sin empresa, {@code general = true} y
     * {@code enabled = false}.
     */
    public static VaccinationType generalDeshabilitada() {
        return new VaccinationType(GENERAL_TYPE_ID, "Vacuna universal", "Disponible para todas",
                null, true, CREADO, null, false);
    }

    public static CreateVaccinationTypeCommand comandoCrear() {
        return new CreateVaccinationTypeCommand("Rabia", "Vacuna antirrabica", COMPANY_ID, false);
    }

    /** Alta en el catalogo de plataforma: sin empresa y general. */
    public static CreateVaccinationTypeCommand comandoCrearGeneral() {
        return new CreateVaccinationTypeCommand("Vacuna universal", "Disponible para todas", null,
                true);
    }

    public static UpdateVaccinationTypeCommand comandoActualizar() {
        return new UpdateVaccinationTypeCommand(TYPE_ID, "Moquillo", "Vacuna contra el moquillo",
                COMPANY_ID, false);
    }

    /** Edicion por el camino SYSTEM: sin empresa en el command. */
    public static UpdateVaccinationTypeCommand comandoActualizarGeneral() {
        return new UpdateVaccinationTypeCommand(TYPE_ID, "Vacuna universal",
                "Disponible para todas", null, true);
    }
}
