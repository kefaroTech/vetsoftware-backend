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
                true, CREATED_DATE, null, true);
    }

    /** Tipo privado de una empresa concreta. */
    public static DiagnosticImagingType propiaDeEmpresa() {
        return new DiagnosticImagingType(TYPE_ID, "Ecografia abdominal", "Ecografia de rutina",
                EMPRESA, false, CREATED_DATE, null, true);
    }

    public static DiagnosticImagingType deshabilitada() {
        return new DiagnosticImagingType(TYPE_ID, "Tomografia", "Tomografia computarizada", EMPRESA,
                false, CREATED_DATE, null, false);
    }

    /**
     * Tipo GLOBAL dado de baja: el que ocupa un nombre del catalogo de plataforma.
     */
    public static DiagnosticImagingType generalDeshabilitada() {
        return new DiagnosticImagingType(TYPE_ID, "Radiografia", "Radiografia simple digital", null,
                true, CREATED_DATE, null, false);
    }

    public static CreateDiagnosticImagingTypeCommand comandoCrearGeneral() {
        return new CreateDiagnosticImagingTypeCommand("Radiografia", "Radiografia simple digital",
                null, true);
    }

    public static CreateDiagnosticImagingTypeCommand comandoCrearDeEmpresa() {
        return new CreateDiagnosticImagingTypeCommand("Ecografia abdominal", "Ecografia de rutina",
                COMPANY_ID, false);
    }

    /**
     * Descripcion distinta de la que llevan las filas de arriba: es lo que deja ver
     * que la reactivacion REESCRIBE los detalles y no se limita a subir
     * {@code enabled}.
     */
    public static final String DESCRIPCION_NUEVA = "Tomografia con medio de contraste";

    public static final String DESCRIPCION_GENERAL_NUEVA = "Radiografia digital de alta resolucion";

    /**
     * Alta que reutiliza el nombre de la fila dada de baja, con descripcion nueva.
     */
    public static CreateDiagnosticImagingTypeCommand comandoCrearTomografia() {
        return new CreateDiagnosticImagingTypeCommand("Tomografia", DESCRIPCION_NUEVA, COMPANY_ID,
                false);
    }

    public static CreateDiagnosticImagingTypeCommand comandoCrearGeneralConDescripcionNueva() {
        return new CreateDiagnosticImagingTypeCommand("Radiografia", DESCRIPCION_GENERAL_NUEVA,
                null, true);
    }

    /**
     * Alta incoherente: declara empresa Y {@code general = true} a la vez. El XOR
     * del dominio la rechaza, y por eso el {@code update} va antes del UPDATE
     * nativo de reactivacion.
     */
    public static CreateDiagnosticImagingTypeCommand comandoCrearIncoherente() {
        return new CreateDiagnosticImagingTypeCommand("Tomografia", DESCRIPCION_NUEVA, COMPANY_ID,
                true);
    }

    public static UpdateDiagnosticImagingTypeCommand comandoActualizar() {
        return new UpdateDiagnosticImagingTypeCommand(TYPE_ID, "Ecografia abdominal (actualizada)",
                "Ecografia de rutina actualizada", COMPANY_ID, false);
    }

    /**
     * Edicion por el camino SYSTEM: sin empresa, sobre el catalogo de plataforma.
     */
    public static UpdateDiagnosticImagingTypeCommand comandoActualizarGeneral() {
        return new UpdateDiagnosticImagingTypeCommand(TYPE_ID, "Radiografia",
                DESCRIPCION_GENERAL_NUEVA, null, true);
    }
}
