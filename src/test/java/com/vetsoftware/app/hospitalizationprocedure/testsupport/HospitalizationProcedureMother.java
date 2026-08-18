package com.vetsoftware.app.hospitalizationprocedure.testsupport;

import com.vetsoftware.app.hospitalizationprocedure.application.command.CreateHospitalizationProcedureCommand;
import com.vetsoftware.app.hospitalizationprocedure.application.command.SuspendHospitalizationProcedureCommand;
import com.vetsoftware.app.hospitalizationprocedure.application.command.UpdateHospitalizationProcedureCommand;
import com.vetsoftware.app.hospitalizationprocedure.domain.DurationMeasure;
import com.vetsoftware.app.hospitalizationprocedure.domain.EmployeeRef;
import com.vetsoftware.app.hospitalizationprocedure.domain.Frequency;
import com.vetsoftware.app.hospitalizationprocedure.domain.GuidelineType;
import com.vetsoftware.app.hospitalizationprocedure.domain.HospitalizationProcedure;
import com.vetsoftware.app.hospitalizationprocedure.domain.HospitalizationRef;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Fixtures de la feature hospitalizationprocedure.
 *
 * <p>
 * Se construyen con el constructor publico y no con
 * {@code HospitalizationProcedure.create(...)}: el factory pone
 * {@code LocalDateTime.now()} y haria no deterministas las aserciones sobre
 * {@code createdDate}.
 */
public final class HospitalizationProcedureMother {

    public static final Long PROCEDURE_ID = 500L;
    public static final Long COMPANY_ID = 9L;
    /** Empresa ajena: el tenant contra el que se prueba el aislamiento. */
    public static final Long OTRA_COMPANY_ID = 77L;
    public static final Long HOSPITALIZATION_ID = 20L;
    public static final Long EMPLOYEE_ID = 4L;
    public static final Long OTHER_EMPLOYEE_ID = 5L;

    public static final HospitalizationRef HOSPITALIZACION = new HospitalizationRef(
            HOSPITALIZATION_ID, LocalDate.of(2026, 3, 1));
    public static final EmployeeRef CREADO_POR = new EmployeeRef(EMPLOYEE_ID, "EMP-001",
            "Ana Ruiz");
    public static final EmployeeRef SUSPENDIDO_POR = new EmployeeRef(OTHER_EMPLOYEE_ID, "EMP-002",
            "Luis Paz");

    public static final LocalDateTime CREADO = LocalDateTime.of(2026, 3, 1, 8, 0);
    public static final LocalDateTime SUSPENDIDO_EL = LocalDateTime.of(2026, 3, 2, 9, 0);

    private HospitalizationProcedureMother() {
    }

    /** Orden activa, con plan completo. El caso por defecto. */
    public static HospitalizationProcedure activo() {
        return activo(PROCEDURE_ID);
    }

    public static HospitalizationProcedure activo(Long id) {
        return new HospitalizationProcedure(id, "Curacion de herida", "Solucion salina 0.9%",
                Frequency.EVERY_8H, GuidelineType.INTERVAL, DurationMeasure.DAYS, 5,
                LocalDate.of(2026, 3, 1), LocalTime.of(8, 0), "Cambiar el vendaje cada turno",
                HOSPITALIZACION, CREADO_POR, CREADO, true, null, null);
    }

    /** Orden ya suspendida por un segundo empleado. */
    public static HospitalizationProcedure suspendido() {
        return new HospitalizationProcedure(PROCEDURE_ID, "Curacion de herida",
                "Solucion salina 0.9%", Frequency.EVERY_8H, GuidelineType.INTERVAL,
                DurationMeasure.DAYS, 5, LocalDate.of(2026, 3, 1), LocalTime.of(8, 0),
                "Cambiar el vendaje cada turno", HOSPITALIZACION, CREADO_POR, CREADO, true,
                SUSPENDIDO_EL, SUSPENDIDO_POR);
    }

    public static HospitalizationProcedure deshabilitado() {
        return new HospitalizationProcedure(PROCEDURE_ID, "Curacion de herida",
                "Solucion salina 0.9%", Frequency.EVERY_8H, GuidelineType.INTERVAL,
                DurationMeasure.DAYS, 5, LocalDate.of(2026, 3, 1), LocalTime.of(8, 0),
                "Cambiar el vendaje cada turno", HOSPITALIZACION, CREADO_POR, CREADO, false, null,
                null);
    }

    /**
     * Sin ninguno de los campos opcionales de planificacion: solo lo obligatorio.
     */
    public static HospitalizationProcedure sinDetallesOpcionales() {
        return new HospitalizationProcedure(PROCEDURE_ID, "Curacion de herida", null, null, null,
                null, null, null, null, null, HOSPITALIZACION, CREADO_POR, CREADO, true, null,
                null);
    }

    public static CreateHospitalizationProcedureCommand comandoCrear() {
        return new CreateHospitalizationProcedureCommand("Curacion de herida",
                "Solucion salina 0.9%", "EVERY_8H", "INTERVAL", "DAYS", 5, LocalDate.of(2026, 3, 1),
                LocalTime.of(8, 0), "Cambiar el vendaje cada turno", HOSPITALIZATION_ID,
                EMPLOYEE_ID);
    }

    public static UpdateHospitalizationProcedureCommand comandoActualizar() {
        return comandoActualizar(COMPANY_ID);
    }

    /** El mismo comando dirigido a otra empresa: el caso de fuga entre tenants. */
    public static UpdateHospitalizationProcedureCommand comandoActualizar(Long companyId) {
        return new UpdateHospitalizationProcedureCommand(PROCEDURE_ID, "Curacion actualizada",
                "Suero fisiologico", "EVERY_12H", "FIXED", "DOSES", 3, LocalDate.of(2026, 3, 2),
                LocalTime.of(9, 0), "Notas actualizadas", companyId);
    }

    public static SuspendHospitalizationProcedureCommand comandoSuspender() {
        return comandoSuspender(COMPANY_ID);
    }

    /** El mismo comando dirigido a otra empresa: el caso de fuga entre tenants. */
    public static SuspendHospitalizationProcedureCommand comandoSuspender(Long companyId) {
        return new SuspendHospitalizationProcedureCommand(PROCEDURE_ID, OTHER_EMPLOYEE_ID,
                companyId);
    }
}
