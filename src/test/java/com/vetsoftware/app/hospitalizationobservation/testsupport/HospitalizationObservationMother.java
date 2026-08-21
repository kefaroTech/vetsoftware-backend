package com.vetsoftware.app.hospitalizationobservation.testsupport;

import com.vetsoftware.app.hospitalizationobservation.application.command.CreateHospitalizationObservationCommand;
import com.vetsoftware.app.hospitalizationobservation.application.command.UpdateHospitalizationObservationCommand;
import com.vetsoftware.app.hospitalizationobservation.domain.EmployeeRef;
import com.vetsoftware.app.hospitalizationobservation.domain.HospitalizationObservation;
import com.vetsoftware.app.hospitalizationobservation.domain.HospitalizationRef;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Fixtures de la feature hospitalizationobservation.
 *
 * <p>
 * Se construyen con el constructor publico de
 * {@link HospitalizationObservation} y no con
 * {@code HospitalizationObservation.create(...)}: el factory pone
 * {@code LocalDateTime.now()} y haria no deterministas las aserciones sobre
 * {@code createdDate}.
 */
public final class HospitalizationObservationMother {

    public static final Long OBSERVATION_ID = 800L;
    public static final Long OTRA_OBSERVATION_ID = 801L;
    public static final Long COMPANY_ID = 9L;
    /** Empresa ajena: el tenant contra el que se prueba el aislamiento. */
    public static final Long OTRA_COMPANY_ID = 77L;
    public static final Long HOSPITALIZATION_ID = 600L;
    public static final Long OTRA_HOSPITALIZATION_ID = 601L;
    public static final Long EMPLOYEE_ID = 4L;

    public static final EmployeeRef VETERINARIO = new EmployeeRef(EMPLOYEE_ID, "EMP-001",
            "Ana Ruiz");
    public static final EmployeeRef OTRO_VETERINARIO = new EmployeeRef(5L, "EMP-002", "Luis Paz");
    public static final HospitalizationRef HOSPITALIZACION = new HospitalizationRef(
            HOSPITALIZATION_ID, LocalDate.of(2026, 3, 1));
    public static final HospitalizationRef OTRA_HOSPITALIZACION = new HospitalizationRef(
            OTRA_HOSPITALIZATION_ID, LocalDate.of(2026, 3, 2));

    public static final LocalDateTime CREADO = LocalDateTime.of(2026, 3, 1, 10, 30);
    public static final String DESCRIPCION = "Paciente estable, sin novedades";

    private HospitalizationObservationMother() {
    }

    /** Observacion valida, tal como quedaria persistida. El caso por defecto. */
    public static HospitalizationObservation observacionValida() {
        return observacionValida(OBSERVATION_ID);
    }

    public static HospitalizationObservation observacionValida(Long id) {
        return new HospitalizationObservation(id, DESCRIPCION, HOSPITALIZACION, VETERINARIO, CREADO,
                null, true);
    }

    public static CreateHospitalizationObservationCommand comandoCrear() {
        return comandoCrear(COMPANY_ID);
    }

    /** El mismo comando dirigido a otra empresa: el caso de fuga entre tenants. */
    public static CreateHospitalizationObservationCommand comandoCrear(Long companyId) {
        return new CreateHospitalizationObservationCommand(DESCRIPCION, HOSPITALIZATION_ID,
                EMPLOYEE_ID, companyId);
    }

    public static UpdateHospitalizationObservationCommand comandoActualizar() {
        return comandoActualizar(COMPANY_ID);
    }

    /** El mismo comando dirigido a otra empresa: el caso de fuga entre tenants. */
    public static UpdateHospitalizationObservationCommand comandoActualizar(Long companyId) {
        return new UpdateHospitalizationObservationCommand(OBSERVATION_ID,
                "Descripcion actualizada", companyId);
    }
}
