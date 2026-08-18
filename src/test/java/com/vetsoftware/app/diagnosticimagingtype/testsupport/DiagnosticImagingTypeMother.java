package com.vetsoftware.app.diagnosticimagingtype.testsupport;

import com.vetsoftware.app.diagnosticimagingtype.application.command.CreateDiagnosticImagingTypeCommand;
import com.vetsoftware.app.diagnosticimagingtype.application.command.UpdateDiagnosticImagingTypeCommand;
import com.vetsoftware.app.diagnosticimagingtype.domain.CompanyRef;
import com.vetsoftware.app.diagnosticimagingtype.domain.DiagnosticImagingType;
import java.time.LocalDateTime;

public final class DiagnosticImagingTypeMother {

    public static final Long TYPE_ID = 501L;
    public static final Long COMPANY_ID = 9L;
    public static final LocalDateTime CREATED_DATE = LocalDateTime.of(2026, 1, 15, 10, 0);

    public static final CompanyRef EMPRESA = new CompanyRef(COMPANY_ID, "Clinica Norte",
            "900123456");

    private DiagnosticImagingTypeMother() {
    }

    /** Tipo global, disponible para todas las empresas (sin company asociada). */
    public static DiagnosticImagingType general() {
        return new DiagnosticImagingType(TYPE_ID, "Radiografia", "Radiografia simple digital", null,
                true, CREATED_DATE, true);
    }

    /** Tipo privado de una empresa concreta. */
    public static DiagnosticImagingType propiaDeEmpresa() {
        return new DiagnosticImagingType(TYPE_ID, "Ecografia abdominal", "Ecografia de rutina",
                EMPRESA, false, CREATED_DATE, true);
    }

    public static DiagnosticImagingType deshabilitada() {
        return new DiagnosticImagingType(TYPE_ID, "Tomografia", "Tomografia computarizada", EMPRESA,
                false, CREATED_DATE, false);
    }

    public static CreateDiagnosticImagingTypeCommand comandoCrearGeneral() {
        return new CreateDiagnosticImagingTypeCommand("Radiografia", "Radiografia simple digital",
                null, true);
    }

    public static CreateDiagnosticImagingTypeCommand comandoCrearDeEmpresa() {
        return new CreateDiagnosticImagingTypeCommand("Ecografia abdominal", "Ecografia de rutina",
                COMPANY_ID, false);
    }

    public static UpdateDiagnosticImagingTypeCommand comandoActualizar() {
        return new UpdateDiagnosticImagingTypeCommand(TYPE_ID, "Ecografia abdominal (actualizada)",
                "Ecografia de rutina actualizada", COMPANY_ID, false);
    }
}
