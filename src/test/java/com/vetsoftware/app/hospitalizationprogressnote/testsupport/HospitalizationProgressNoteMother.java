package com.vetsoftware.app.hospitalizationprogressnote.testsupport;

import com.vetsoftware.app.hospitalizationprogressnote.application.command.CreateHospitalizationProgressNoteCommand;
import com.vetsoftware.app.hospitalizationprogressnote.application.command.UpdateHospitalizationProgressNoteCommand;
import com.vetsoftware.app.hospitalizationprogressnote.domain.EmployeeRef;
import com.vetsoftware.app.hospitalizationprogressnote.domain.HospitalizationProgressNote;
import com.vetsoftware.app.hospitalizationprogressnote.domain.HospitalizationRef;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Fixtures de la feature hospitalizationprogressnote.
 *
 * <p>
 * Se construyen con el constructor publico y no con {@code create(...)}: el
 * factory pone {@code LocalDateTime.now()} y haria no deterministas las
 * aserciones sobre {@code createdDate}.
 */
public final class HospitalizationProgressNoteMother {

    public static final Long NOTE_ID = 500L;
    public static final Long HOSPITALIZATION_ID = 55L;
    public static final Long EMPLOYEE_ID = 4L;
    public static final Long COMPANY_ID = 9L;
    /** Empresa ajena: el tenant contra el que se prueba el aislamiento. */
    public static final Long OTRA_COMPANY_ID = 77L;

    public static final HospitalizationRef HOSPITALIZACION = new HospitalizationRef(
            HOSPITALIZATION_ID, LocalDate.of(2026, 3, 1));
    public static final EmployeeRef VETERINARIO = new EmployeeRef(EMPLOYEE_ID, "EMP-001",
            "Ana Ruiz");

    public static final LocalDateTime CREADO = LocalDateTime.of(2026, 3, 1, 9, 15);

    private static final String DESCRIPCION = "Paciente estable, buena respuesta al tratamiento";

    private HospitalizationProgressNoteMother() {
    }

    /** Nota de evolucion habilitada, con id ya asignado (simula lo persistido). */
    public static HospitalizationProgressNote notaEvolucion() {
        return notaEvolucion(NOTE_ID);
    }

    public static HospitalizationProgressNote notaEvolucion(Long id) {
        return new HospitalizationProgressNote(id, DESCRIPCION, HOSPITALIZACION, VETERINARIO,
                CREADO, null, true);
    }

    public static HospitalizationProgressNote deshabilitada() {
        return new HospitalizationProgressNote(NOTE_ID, "Nota deshabilitada", HOSPITALIZACION,
                VETERINARIO, CREADO, null, false);
    }

    /** Comando de creacion coherente con las refs de arriba. */
    public static CreateHospitalizationProgressNoteCommand comandoCrear() {
        return comandoCrear(COMPANY_ID);
    }

    /** El mismo comando dirigido a otra empresa: el caso de fuga entre tenants. */
    public static CreateHospitalizationProgressNoteCommand comandoCrear(Long companyId) {
        return new CreateHospitalizationProgressNoteCommand(DESCRIPCION, HOSPITALIZATION_ID,
                EMPLOYEE_ID, companyId);
    }

    public static UpdateHospitalizationProgressNoteCommand comandoActualizar() {
        return comandoActualizar(COMPANY_ID);
    }

    /** El mismo comando dirigido a otra empresa: el caso de fuga entre tenants. */
    public static UpdateHospitalizationProgressNoteCommand comandoActualizar(Long companyId) {
        return new UpdateHospitalizationProgressNoteCommand(NOTE_ID,
                "Evolucion favorable, se ajusta analgesia", companyId);
    }
}
