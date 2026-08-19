package com.vetsoftware.app.diagnosticimaging.testsupport;

import com.vetsoftware.app.diagnosticimaging.application.command.ChangeDiagnosticImagingStatusCommand;
import com.vetsoftware.app.diagnosticimaging.application.command.CreateDiagnosticImagingCommand;
import com.vetsoftware.app.diagnosticimaging.application.command.UpdateDiagnosticImagingCommand;
import com.vetsoftware.app.diagnosticimaging.domain.AnimalRef;
import com.vetsoftware.app.diagnosticimaging.domain.CompanyRef;
import com.vetsoftware.app.diagnosticimaging.domain.ConsultationRef;
import com.vetsoftware.app.diagnosticimaging.domain.DiagnosticImaging;
import com.vetsoftware.app.diagnosticimaging.domain.DiagnosticImagingStatus;
import com.vetsoftware.app.diagnosticimaging.domain.DiagnosticImagingTypeRef;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Fixtures del modulo diagnosticimaging. */
public final class DiagnosticImagingMother {

    public static final Long IMAGING_ID = 700L;
    public static final Long COMPANY_ID = 9L;
    public static final Long ANIMAL_ID = 701L;
    public static final Long CONSULTATION_ID = 702L;
    public static final Long TYPE_ID = 703L;

    public static final LocalDate FECHA = LocalDate.of(2026, 1, 10);
    public static final LocalDateTime CREADO = LocalDateTime.of(2026, 1, 10, 9, 0);

    public static final CompanyRef EMPRESA = new CompanyRef(COMPANY_ID, "Clinica Norte",
            "900123456");
    public static final CompanyRef OTRA_EMPRESA = new CompanyRef(10L, "Clinica Sur", "900654321");
    public static final AnimalRef MASCOTA = new AnimalRef(ANIMAL_ID, "Firulais", "A-001");
    public static final ConsultationRef CONSULTA = new ConsultationRef(CONSULTATION_ID, FECHA);
    public static final DiagnosticImagingTypeRef TIPO = new DiagnosticImagingTypeRef(TYPE_ID,
            "Radiografia");

    private DiagnosticImagingMother() {
    }

    /** Imagen valida recien creada, con consulta asociada. */
    public static DiagnosticImaging valida() {
        return DiagnosticImaging.create(FECHA, TIPO, "Cojera pata trasera", "Radiografia de cadera",
                "Displasia leve", "Control en 30 dias", MASCOTA, CONSULTA, EMPRESA);
    }

    /** Imagen valida sin consulta asociada: es opcional. */
    public static DiagnosticImaging sinConsulta() {
        return DiagnosticImaging.create(FECHA, TIPO, "Cojera pata trasera", "Radiografia de cadera",
                "Displasia leve", "Control en 30 dias", MASCOTA, null, EMPRESA);
    }

    /** Imagen ya persistida, en PENDIENTE y habilitada. El caso por defecto. */
    public static DiagnosticImaging persistida() {
        return new DiagnosticImaging(IMAGING_ID, FECHA, TIPO, "Cojera pata trasera",
                "Radiografia de cadera", "Displasia leve", "Control en 30 dias",
                DiagnosticImagingStatus.PENDIENTE, MASCOTA, CONSULTA, EMPRESA, CREADO, null, true);
    }

    /** Imagen ya persistida, cancelada y deshabilitada. */
    public static DiagnosticImaging deshabilitada() {
        return new DiagnosticImaging(IMAGING_ID, FECHA, TIPO, "Cojera pata trasera",
                "Radiografia de cadera", "Displasia leve", "Control en 30 dias",
                DiagnosticImagingStatus.CANCELADO, MASCOTA, CONSULTA, EMPRESA, CREADO, null, false);
    }

    public static CreateDiagnosticImagingCommand comandoCrear() {
        return new CreateDiagnosticImagingCommand(FECHA, TYPE_ID, "Cojera pata trasera",
                "Radiografia de cadera", "Displasia leve", "Control en 30 dias", ANIMAL_ID,
                CONSULTATION_ID, COMPANY_ID);
    }

    public static UpdateDiagnosticImagingCommand comandoActualizar() {
        return new UpdateDiagnosticImagingCommand(IMAGING_ID, FECHA, TYPE_ID, "Cojera pata trasera",
                "Radiografia de cadera actualizada", "Displasia moderada", "Control en 15 dias",
                ANIMAL_ID, CONSULTATION_ID, COMPANY_ID);
    }

    public static ChangeDiagnosticImagingStatusCommand comandoCambiarEstado(String status) {
        return new ChangeDiagnosticImagingStatusCommand(IMAGING_ID, status, COMPANY_ID);
    }
}
