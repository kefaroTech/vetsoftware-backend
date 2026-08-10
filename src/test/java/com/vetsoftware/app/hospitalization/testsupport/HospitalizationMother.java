package com.vetsoftware.app.hospitalization.testsupport;

import com.vetsoftware.app.hospitalization.application.command.CreateHospitalizationCommand;
import com.vetsoftware.app.hospitalization.application.command.UpdateHospitalizationCommand;
import com.vetsoftware.app.hospitalization.domain.AnimalRef;
import com.vetsoftware.app.hospitalization.domain.CompanyRef;
import com.vetsoftware.app.hospitalization.domain.ConsultationRef;
import com.vetsoftware.app.hospitalization.domain.Hospitalization;
import com.vetsoftware.app.hospitalization.domain.HospitalizationType;
import com.vetsoftware.app.hospitalization.domain.ReasonLeaving;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Fixtures de la feature. Valores validos por defecto y un metodo por variante:
 * cada test cambia solo el campo que esta probando.
 */
public final class HospitalizationMother {

    public static final Long HOSPITALIZATION_ID = 55L;
    public static final Long COMPANY_ID = 9L;
    public static final Long ANIMAL_ID = 3L;
    public static final Long CONSULTATION_ID = 7L;

    public static final AnimalRef FIRULAIS = new AnimalRef(ANIMAL_ID, "Firulais", "A-001");
    public static final ConsultationRef CONSULTA = new ConsultationRef(CONSULTATION_ID,
            LocalDate.of(2026, 2, 28));
    public static final CompanyRef CLINICA = new CompanyRef(COMPANY_ID, "Clinica Vet", "900123456");

    public static final LocalDate FECHA = LocalDate.of(2026, 3, 1);
    public static final LocalDate INICIO = LocalDate.of(2026, 3, 1);
    public static final LocalDate FIN = LocalDate.of(2026, 3, 5);
    public static final LocalDateTime CREADO = LocalDateTime.of(2026, 3, 1, 9, 15);

    private HospitalizationMother() {
    }

    /** Hospitalizacion persistida, con consulta asociada y habilitada. */
    public static Hospitalization internado() {
        return new Hospitalization(HOSPITALIZATION_ID, FECHA, INICIO, FIN,
                HospitalizationType.HOSPITALIZATION, ReasonLeaving.MEDICAL_DISCHARGE,
                "Gastroenteritis aguda", "Sin complicaciones", FIRULAIS, CONSULTA, CLINICA, CREADO,
                true);
    }

    /** Variante ambulatoria: sin consulta, sin motivo de salida y sin fecha fin. */
    public static Hospitalization ambulatorioSinConsulta() {
        return new Hospitalization(HOSPITALIZATION_ID, FECHA, INICIO, null,
                HospitalizationType.OUTPATIENT, null, "Control post quirurgico", null, FIRULAIS,
                null, CLINICA, CREADO, true);
    }

    /** Misma hospitalizacion pero deshabilitada (soft delete aplicado). */
    public static Hospitalization deshabilitado() {
        Hospitalization hospitalization = internado();
        hospitalization.disable();
        return hospitalization;
    }

    public static CreateHospitalizationCommand comandoCrear() {
        return new CreateHospitalizationCommand(FECHA, INICIO, FIN,
                HospitalizationType.HOSPITALIZATION, ReasonLeaving.MEDICAL_DISCHARGE,
                "Gastroenteritis aguda", "Sin complicaciones", ANIMAL_ID, CONSULTATION_ID,
                COMPANY_ID, null, null);
    }

    public static CreateHospitalizationCommand comandoCrearSinConsulta() {
        return new CreateHospitalizationCommand(FECHA, INICIO, FIN,
                HospitalizationType.HOSPITALIZATION, ReasonLeaving.MEDICAL_DISCHARGE,
                "Gastroenteritis aguda", "Sin complicaciones", ANIMAL_ID, null, COMPANY_ID, null,
                null);
    }

    public static CreateHospitalizationCommand comandoCrearConPeso(BigDecimal peso, String unidad) {
        return new CreateHospitalizationCommand(FECHA, INICIO, FIN,
                HospitalizationType.HOSPITALIZATION, ReasonLeaving.MEDICAL_DISCHARGE,
                "Gastroenteritis aguda", "Sin complicaciones", ANIMAL_ID, CONSULTATION_ID,
                COMPANY_ID, peso, unidad);
    }

    public static UpdateHospitalizationCommand comandoActualizar() {
        return new UpdateHospitalizationCommand(HOSPITALIZATION_ID, FECHA, INICIO, FIN,
                HospitalizationType.HOSPITALIZATION, ReasonLeaving.HOME_TREATMENT,
                "Gastroenteritis controlada", "Alta con dieta", ANIMAL_ID, CONSULTATION_ID,
                COMPANY_ID);
    }

    public static UpdateHospitalizationCommand comandoActualizarSinConsulta() {
        return new UpdateHospitalizationCommand(HOSPITALIZATION_ID, FECHA, INICIO, FIN,
                HospitalizationType.HOSPITALIZATION, ReasonLeaving.HOME_TREATMENT,
                "Gastroenteritis controlada", "Alta con dieta", ANIMAL_ID, null, COMPANY_ID);
    }
}
