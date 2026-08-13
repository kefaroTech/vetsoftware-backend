package com.vetsoftware.app.medicationschedule.testsupport;

import com.vetsoftware.app.medicationschedule.domain.AppliedStatus;
import com.vetsoftware.app.medicationschedule.domain.EmployeeRef;
import com.vetsoftware.app.medicationschedule.domain.HospitalizationMedicationRef;
import com.vetsoftware.app.medicationschedule.domain.MedicationOrderParams;
import com.vetsoftware.app.medicationschedule.domain.MedicationSchedule;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Fixtures del modulo medicationschedule.
 *
 * <p>
 * La orden de referencia es amoxicilina cada 8 horas durante 3 dias empezando
 * el 15 de enero a las 08:00: nueve tomas a las 08:00, 16:00 y 00:00. Numeros
 * elegidos para que las horas se comprueben de cabeza.
 */
public final class MedicationScheduleMother {

    public static final Long MEDICATION_ID = 300L;
    public static final Long HOSPITALIZATION_ID = 400L;
    public static final Long SCHEDULE_ID = 500L;

    public static final EmployeeRef EMPLEADO = new EmployeeRef(7L, "EMP-001", "Ana Ruiz");
    public static final HospitalizationMedicationRef ORDEN = new HospitalizationMedicationRef(
            MEDICATION_ID, "Amoxicilina 500mg");

    public static final LocalDate INICIO = LocalDate.of(2026, 1, 15);
    public static final LocalTime HORA_INICIO = LocalTime.of(8, 0);
    public static final LocalDateTime PRIMERA_TOMA = LocalDateTime.of(2026, 1, 15, 8, 0);
    public static final LocalDateTime CREADO = LocalDateTime.of(2026, 1, 15, 7, 30);

    private MedicationScheduleMother() {
    }

    /** Toma pendiente a la hora prevista. */
    public static MedicationSchedule tomaPendiente() {
        return new MedicationSchedule(SCHEDULE_ID, ORDEN, PRIMERA_TOMA, PRIMERA_TOMA, null,
                AppliedStatus.PENDING, false, EMPLEADO, CREADO, true);
    }

    /** Cada 8 horas durante 3 dias: nueve tomas. */
    public static MedicationOrderParams cada8hDurante3Dias() {
        return orden("EVERY_8H", "FIXED", "DAYS", 3);
    }

    public static MedicationOrderParams orden(String frecuencia, String tipoPauta, String medida,
            Integer cantidad) {
        return new MedicationOrderParams(MEDICATION_ID, "Amoxicilina 500mg", HOSPITALIZATION_ID,
                frecuencia, tipoPauta, medida, cantidad, INICIO, HORA_INICIO);
    }

    /** Orden sin hora de inicio: el generador tiene que poner la de por defecto. */
    public static MedicationOrderParams sinHoraDeInicio(String frecuencia) {
        return new MedicationOrderParams(MEDICATION_ID, "Amoxicilina 500mg", HOSPITALIZATION_ID,
                frecuencia, "FIXED", "DAYS", 1, INICIO, null);
    }

    /** Orden sin fecha de inicio: no se puede agendar nada. */
    public static MedicationOrderParams sinFechaDeInicio() {
        return new MedicationOrderParams(MEDICATION_ID, "Amoxicilina 500mg", HOSPITALIZATION_ID,
                "EVERY_8H", "FIXED", "DAYS", 3, null, HORA_INICIO);
    }
}
