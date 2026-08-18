package com.vetsoftware.app.procedureschedule.testsupport;

import com.vetsoftware.app.procedureschedule.domain.AppliedStatus;
import com.vetsoftware.app.procedureschedule.domain.EmployeeRef;
import com.vetsoftware.app.procedureschedule.domain.HospitalizationProcedureRef;
import com.vetsoftware.app.procedureschedule.domain.ProcedureOrderParams;
import com.vetsoftware.app.procedureschedule.domain.ProcedureSchedule;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Fixtures del modulo procedureschedule.
 *
 * <p>
 * La orden de referencia es una curacion de herida cada 8 horas durante 3 dias
 * empezando el 15 de enero a las 08:00: nueve tomas a las 08:00, 16:00 y 00:00.
 * Numeros elegidos para que las horas se comprueben de cabeza.
 */
public final class ProcedureScheduleMother {

    public static final Long PROCEDURE_ID = 300L;
    public static final Long HOSPITALIZATION_ID = 400L;
    public static final Long SCHEDULE_ID = 500L;

    public static final EmployeeRef EMPLEADO = new EmployeeRef(7L, "EMP-001", "Ana Ruiz");
    public static final HospitalizationProcedureRef ORDEN = new HospitalizationProcedureRef(
            PROCEDURE_ID, "Curacion de herida");

    public static final LocalDate INICIO = LocalDate.of(2026, 1, 15);
    public static final LocalTime HORA_INICIO = LocalTime.of(8, 0);
    public static final LocalDateTime PRIMERA_TOMA = LocalDateTime.of(2026, 1, 15, 8, 0);
    public static final LocalDateTime CREADO = LocalDateTime.of(2026, 1, 15, 7, 30);

    private ProcedureScheduleMother() {
    }

    /** Toma pendiente a la hora prevista. */
    public static ProcedureSchedule tomaPendiente() {
        return new ProcedureSchedule(SCHEDULE_ID, ORDEN, PRIMERA_TOMA, PRIMERA_TOMA, null,
                AppliedStatus.PENDING, false, EMPLEADO, CREADO, true);
    }

    /** Cada 8 horas durante 3 dias: nueve tomas. */
    public static ProcedureOrderParams cada8hDurante3Dias() {
        return orden("EVERY_8H", "FIXED", "DAYS", 3);
    }

    public static ProcedureOrderParams orden(String frecuencia, String tipoPauta, String medida,
            Integer cantidad) {
        return new ProcedureOrderParams(PROCEDURE_ID, "Curacion de herida", HOSPITALIZATION_ID,
                frecuencia, tipoPauta, medida, cantidad, INICIO, HORA_INICIO);
    }

    /** Orden sin hora de inicio: el generador tiene que poner la de por defecto. */
    public static ProcedureOrderParams sinHoraDeInicio(String frecuencia) {
        return new ProcedureOrderParams(PROCEDURE_ID, "Curacion de herida", HOSPITALIZATION_ID,
                frecuencia, "FIXED", "DAYS", 1, INICIO, null);
    }

    /** Orden sin fecha de inicio: no se puede agendar nada. */
    public static ProcedureOrderParams sinFechaDeInicio() {
        return new ProcedureOrderParams(PROCEDURE_ID, "Curacion de herida", HOSPITALIZATION_ID,
                "EVERY_8H", "FIXED", "DAYS", 3, null, HORA_INICIO);
    }

    /**
     * Toma con id, hora y estado explicitos — para listas de plan con varias tomas.
     */
    public static ProcedureSchedule toma(Long id, LocalDateTime cuando, AppliedStatus status) {
        return new ProcedureSchedule(id, ORDEN, cuando, cuando, null, status, false, EMPLEADO,
                CREADO, true);
    }

    /** Toma ya aplicada, con su hora real. */
    public static ProcedureSchedule tomaAplicada(Long id, LocalDateTime cuando,
            LocalDateTime horaReal) {
        return new ProcedureSchedule(id, ORDEN, cuando, cuando, horaReal, AppliedStatus.APPLIED,
                false, EMPLEADO, CREADO, true);
    }
}
