package com.vetsoftware.app.surgerytype.testsupport;

import com.vetsoftware.app.surgerytype.application.command.CreateSurgeryTypeCommand;
import com.vetsoftware.app.surgerytype.application.command.UpdateSurgeryTypeCommand;
import com.vetsoftware.app.surgerytype.domain.CompanyRef;
import com.vetsoftware.app.surgerytype.domain.SurgeryType;
import java.time.LocalDateTime;

public final class SurgeryTypeMother {

    public static final Long SURGERY_TYPE_ID = 700L;
    public static final Long GENERAL_SURGERY_TYPE_ID = 701L;
    public static final Long COMPANY_ID = 9L;

    public static final CompanyRef EMPRESA = new CompanyRef(COMPANY_ID, "Clinica Norte",
            "900123456");
    public static final CompanyRef OTRA_EMPRESA = new CompanyRef(10L, "Clinica Sur", "900654321");

    public static final LocalDateTime CREADO = LocalDateTime.of(2026, 3, 12, 9, 15);

    private SurgeryTypeMother() {
    }

    /** Tipo propio de una empresa, activo. El caso por defecto. */
    public static SurgeryType propioDeEmpresa() {
        return new SurgeryType(SURGERY_TYPE_ID, "Castracion", "Cirugia de esterilizacion", EMPRESA,
                false, CREADO, null, true);
    }

    /** Tipo global, sin empresa, disponible para todos los tenants. */
    public static SurgeryType general() {
        return new SurgeryType(GENERAL_SURGERY_TYPE_ID, "Cirugia general", "Procedimiento estandar",
                null, true, CREADO, null, true);
    }

    public static SurgeryType deshabilitado() {
        return new SurgeryType(SURGERY_TYPE_ID, "Castracion", "Cirugia de esterilizacion", EMPRESA,
                false, CREADO, null, false);
    }

    /**
     * Fila del catalogo de PLATAFORMA dada de baja. Es la ocupante del nombre en la
     * rama de reactivacion global: sin empresa, {@code general = true} y
     * {@code enabled = false}.
     */
    public static SurgeryType generalDeshabilitado() {
        return new SurgeryType(GENERAL_SURGERY_TYPE_ID, "Cirugia general", "Procedimiento estandar",
                null, true, CREADO, null, false);
    }

    public static CreateSurgeryTypeCommand comandoCrearPropio() {
        return new CreateSurgeryTypeCommand("Castracion", "Cirugia de esterilizacion", COMPANY_ID,
                false);
    }

    public static CreateSurgeryTypeCommand comandoCrearGeneral() {
        return new CreateSurgeryTypeCommand("Cirugia general", "Procedimiento estandar", null,
                true);
    }

    public static UpdateSurgeryTypeCommand comandoActualizarPropio() {
        return new UpdateSurgeryTypeCommand(SURGERY_TYPE_ID, "Castracion avanzada",
                "Nueva descripcion", COMPANY_ID, false);
    }

    /**
     * Edicion por el camino SYSTEM: sin empresa en el command. Apunta al id de la
     * fila GLOBAL a proposito — desde el arreglo de la expropiacion, ese camino
     * solo alcanza el catalogo de plataforma.
     */
    public static UpdateSurgeryTypeCommand comandoActualizarGeneral() {
        return new UpdateSurgeryTypeCommand(GENERAL_SURGERY_TYPE_ID, "Cirugia general",
                "Procedimiento estandar", null, true);
    }
}
