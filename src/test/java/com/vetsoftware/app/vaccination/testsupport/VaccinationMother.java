package com.vetsoftware.app.vaccination.testsupport;

import com.vetsoftware.app.vaccination.application.command.CreateVaccinationCommand;
import com.vetsoftware.app.vaccination.application.command.UpdateVaccinationCommand;
import com.vetsoftware.app.vaccination.domain.AnimalRef;
import com.vetsoftware.app.vaccination.domain.CompanyRef;
import com.vetsoftware.app.vaccination.domain.ConsultationRef;
import com.vetsoftware.app.vaccination.domain.Vaccination;
import com.vetsoftware.app.vaccination.domain.VaccinationTypeRef;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Fixtures del modulo vaccination.
 *
 * <p>
 * Se construyen con el constructor publico y no con
 * {@code Vaccination.create(...)}: el factory pone {@code LocalDateTime.now()}
 * y haria no deterministas las aserciones sobre {@code createdDate}.
 */
public final class VaccinationMother {

    public static final Long VACCINATION_ID = 100L;
    public static final Long COMPANY_ID = 9L;

    public static final VaccinationTypeRef RABIA = new VaccinationTypeRef(1L, "Rabia");
    public static final AnimalRef FIRULAIS = new AnimalRef(2L, "Firulais", "A-001");
    public static final ConsultationRef CONSULTA = new ConsultationRef(3L,
            LocalDate.of(2026, 1, 10));
    public static final CompanyRef CLINICA = new CompanyRef(COMPANY_ID, "Clinica Norte", "NIT-900");

    public static final VaccinationTypeRef MOQUILLO = new VaccinationTypeRef(11L, "Moquillo");
    public static final AnimalRef MICHI = new AnimalRef(12L, "Michi", "A-002");
    public static final ConsultationRef OTRA_CONSULTA = new ConsultationRef(13L,
            LocalDate.of(2026, 2, 20));

    public static final LocalDate FECHA = LocalDate.of(2026, 1, 15);
    public static final LocalDate PROXIMA = LocalDate.of(2027, 1, 15);
    public static final LocalDateTime CREADO = LocalDateTime.of(2026, 1, 15, 10, 30);

    private VaccinationMother() {
    }

    /** Vacuna habilitada, con consulta asociada. El caso por defecto. */
    public static Vaccination vigente() {
        return vigente(VACCINATION_ID);
    }

    public static Vaccination vigente(Long id) {
        return new Vaccination(id, FECHA, RABIA, "L-2026-A", "Sin reaccion", "Subcutanea", "Cuello",
                PROXIMA, FIRULAIS, CONSULTA, CLINICA, CREADO, null, true);
    }

    /** Vacuna registrada fuera de consulta: la consulta es opcional. */
    public static Vaccination sinConsulta() {
        return new Vaccination(VACCINATION_ID, FECHA, RABIA, "L-2026-A", "Sin reaccion",
                "Subcutanea", "Cuello", PROXIMA, FIRULAIS, null, CLINICA, CREADO, null, true);
    }

    public static Vaccination deshabilitada() {
        return new Vaccination(VACCINATION_ID, FECHA, RABIA, "L-2026-A", "Sin reaccion",
                "Subcutanea", "Cuello", PROXIMA, FIRULAIS, CONSULTA, CLINICA, CREADO, null, false);
    }

    public static CreateVaccinationCommand comandoCrear() {
        return new CreateVaccinationCommand(FECHA, RABIA.id(), "L-2026-A", "Sin reaccion",
                "Subcutanea", "Cuello", PROXIMA, FIRULAIS.id(), CONSULTA.id(), COMPANY_ID);
    }

    public static CreateVaccinationCommand comandoCrearSinConsulta() {
        return new CreateVaccinationCommand(FECHA, RABIA.id(), "L-2026-A", "Sin reaccion",
                "Subcutanea", "Cuello", PROXIMA, FIRULAIS.id(), null, COMPANY_ID);
    }

    /** Comando de actualizacion que cambia todas las referencias y los datos. */
    public static UpdateVaccinationCommand comandoActualizar() {
        return new UpdateVaccinationCommand(VACCINATION_ID, LocalDate.of(2026, 3, 1), MOQUILLO.id(),
                "L-2026-B", "Reaccion leve", "Intramuscular", "Muslo", LocalDate.of(2027, 3, 1),
                MICHI.id(), OTRA_CONSULTA.id(), COMPANY_ID);
    }

    public static UpdateVaccinationCommand comandoActualizarSinConsulta() {
        return new UpdateVaccinationCommand(VACCINATION_ID, LocalDate.of(2026, 3, 1), MOQUILLO.id(),
                "L-2026-B", "Reaccion leve", "Intramuscular", "Muslo", LocalDate.of(2027, 3, 1),
                MICHI.id(), null, COMPANY_ID);
    }
}
