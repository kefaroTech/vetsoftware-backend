package com.vetsoftware.app.consultation.testsupport;

import com.vetsoftware.app.consultation.application.command.CreateConsultationCommand;
import com.vetsoftware.app.consultation.domain.AnimalRef;
import com.vetsoftware.app.consultation.domain.CompanyRef;
import com.vetsoftware.app.consultation.domain.Consultation;
import com.vetsoftware.app.consultation.domain.ConsultationTypeRef;
import com.vetsoftware.app.consultation.domain.PhysicalExam;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Fixtures del modulo consultation.
 *
 * <p>
 * Se construyen con el constructor publico y no con
 * {@code Consultation.create(...)}: el factory pone {@code LocalDateTime.now()}
 * y haria no deterministas las aserciones sobre {@code createdDate}.
 */
public final class ConsultationMother {

    public static final Long CONSULTATION_ID = 200L;
    public static final Long COMPANY_ID = 9L;
    public static final Long ANIMAL_ID = 100L;
    public static final Long CONSULTATION_TYPE_ID = 5L;

    public static final ConsultationTypeRef CONTROL = new ConsultationTypeRef(CONSULTATION_TYPE_ID,
            "Control");
    public static final AnimalRef FIRULAIS = new AnimalRef(ANIMAL_ID, "Firulais", "A-001");
    public static final CompanyRef CLINICA = new CompanyRef(COMPANY_ID, "Clinica Norte", "NIT-900");

    public static final LocalDate FECHA = LocalDate.of(2026, 3, 1);
    public static final LocalDateTime CREADO = LocalDateTime.of(2026, 3, 1, 9, 0);

    private ConsultationMother() {
    }

    public static PhysicalExam examenVacio() {
        return PhysicalExam.empty();
    }

    public static PhysicalExam examenCompleto() {
        return new PhysicalExam(new BigDecimal("38.5"), 90, 22, "Rosadas", "< 2 seg", "Normal", 5,
                2, "Alerta", "Sin hallazgos relevantes");
    }

    public static Consultation consultaValida() {
        return consultaValida(CONSULTATION_ID);
    }

    public static Consultation consultaValida(Long id) {
        return new Consultation(id, FECHA, CONTROL, "Anamnesis del paciente", "Diagnostico",
                "Pronostico reservado", examenCompleto(), FECHA.plusDays(15), FIRULAIS, CLINICA,
                CREADO, null, true);
    }

    public static Consultation sinExamen() {
        return new Consultation(CONSULTATION_ID, FECHA, CONTROL, "Anamnesis del paciente", null,
                null, null, null, FIRULAIS, CLINICA, CREADO, null, true);
    }

    public static Consultation deshabilitada() {
        return new Consultation(CONSULTATION_ID, FECHA, CONTROL, "Anamnesis del paciente", null,
                null, PhysicalExam.empty(), null, FIRULAIS, CLINICA, CREADO, null, false);
    }

    /** Comando de creacion coherente con las refs de arriba. Sin peso inicial. */
    public static CreateConsultationCommand comandoCrear() {
        return comandoCrearConPeso(null, null);
    }

    public static CreateConsultationCommand comandoCrearConPeso(BigDecimal peso, String unidad) {
        return new CreateConsultationCommand(FECHA, CONSULTATION_TYPE_ID, "Anamnesis del paciente",
                "Diagnostico", "Pronostico reservado", FECHA.plusDays(15), ANIMAL_ID, COMPANY_ID,
                peso, unidad, new BigDecimal("38.5"), 90, 22, "Rosadas", "< 2 seg", "Normal", 5, 2,
                "Alerta", "Sin hallazgos relevantes");
    }
}
