package com.vetsoftware.app.numberingresolution.testsupport;

import com.vetsoftware.app.numberingresolution.application.command.CreateNumberingResolutionCommand;
import com.vetsoftware.app.numberingresolution.application.command.UpdateNumberingResolutionCommand;
import com.vetsoftware.app.numberingresolution.domain.CompanyRef;
import com.vetsoftware.app.numberingresolution.domain.ElectronicDocumentType;
import com.vetsoftware.app.numberingresolution.domain.NumberingResolution;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Fixtures del modulo numberingresolution. */
public final class NumberingResolutionMother {

    public static final Long RESOLUTION_ID = 700L;
    public static final Long COMPANY_ID = 9L;
    public static final Long OTRA_COMPANY_ID = 10L;
    public static final Long BRANCH_ID = 3L;

    public static final CompanyRef EMPRESA = new CompanyRef(COMPANY_ID, "Veterinaria Central",
            "900123456");
    public static final CompanyRef OTRA_EMPRESA = new CompanyRef(OTRA_COMPANY_ID,
            "Veterinaria del Sur", "900654321");

    public static final LocalDate EXPEDIDA = LocalDate.of(2026, 1, 2);
    public static final LocalDate DESDE = LocalDate.of(2026, 1, 1);
    public static final LocalDate HASTA = LocalDate.of(2026, 12, 31);
    public static final LocalDateTime CREADA = LocalDateTime.of(2026, 1, 2, 9, 0, 0);

    private NumberingResolutionMother() {
    }

    /** Resolucion activa de EMPRESA (sin sede), consecutivo al inicio del rango. */
    public static NumberingResolution activaDeEmpresa() {
        return new NumberingResolution(RESOLUTION_ID, EMPRESA, ElectronicDocumentType.FE_VENTA,
                "18760000001", EXPEDIDA, "SETP", 100L, 199L, DESDE, HASTA, "clave-tecnica", 100L,
                CREADA, true, null);
    }

    /** Resolucion activa de una SEDE concreta. */
    public static NumberingResolution activaDeSede() {
        return new NumberingResolution(RESOLUTION_ID, EMPRESA, ElectronicDocumentType.FE_VENTA,
                "18760000001", EXPEDIDA, "SEDE", 100L, 199L, DESDE, HASTA, "clave-tecnica", 100L,
                CREADA, true, BRANCH_ID);
    }

    /** Misma resolucion pero perteneciente a OTRA empresa. */
    public static NumberingResolution activaDeOtraEmpresa() {
        return new NumberingResolution(RESOLUTION_ID, OTRA_EMPRESA, ElectronicDocumentType.FE_VENTA,
                "18760000099", EXPEDIDA, "AJEN", 100L, 199L, DESDE, HASTA, "clave-ajena", 100L,
                CREADA, true, null);
    }

    public static CreateNumberingResolutionCommand comandoCrear() {
        return new CreateNumberingResolutionCommand(ElectronicDocumentType.FE_VENTA, "18760000001",
                EXPEDIDA, "SETP", 100L, 199L, DESDE, HASTA, "clave-tecnica", null, COMPANY_ID);
    }

    public static UpdateNumberingResolutionCommand comandoActualizar() {
        return new UpdateNumberingResolutionCommand(RESOLUTION_ID, ElectronicDocumentType.FE_VENTA,
                "18760000002", EXPEDIDA, "NUEV", 100L, 299L, DESDE, HASTA, "otra-clave", null,
                COMPANY_ID);
    }
}
