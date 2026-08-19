package com.vetsoftware.app.laboratorytesttype.testsupport;

import com.vetsoftware.app.laboratorytesttype.application.command.CreateLaboratoryTestTypeCommand;
import com.vetsoftware.app.laboratorytesttype.application.command.UpdateLaboratoryTestTypeCommand;
import com.vetsoftware.app.laboratorytesttype.domain.CompanyRef;
import com.vetsoftware.app.laboratorytesttype.domain.LaboratoryTestType;
import java.time.LocalDateTime;

/**
 * Fixtures del modulo laboratorytesttype.
 *
 * <p>
 * Los tipos se construyen con el constructor publico y no con
 * {@code LaboratoryTestType.create(...)}: el factory pone
 * {@code LocalDateTime.now()} y haria no deterministas las aserciones sobre
 * {@code createdDate}.
 */
public final class LaboratoryTestTypeMother {

    public static final Long TYPE_ID = 70L;
    public static final Long COMPANY_ID = 9L;
    public static final Long OTRA_COMPANY_ID = 99L;

    public static final CompanyRef CLINICA = new CompanyRef(COMPANY_ID, "Clinica Norte", "NIT-900");
    public static final CompanyRef OTRA_CLINICA = new CompanyRef(OTRA_COMPANY_ID, "Clinica Sur",
            "NIT-990");

    public static final LocalDateTime CREADO = LocalDateTime.of(2026, 1, 15, 10, 30);

    private LaboratoryTestTypeMother() {
    }

    /** Tipo propio de una empresa, activo. El caso por defecto. */
    public static LaboratoryTestType propioDeEmpresa() {
        return propioDeEmpresa(TYPE_ID);
    }

    public static LaboratoryTestType propioDeEmpresa(Long id) {
        return new LaboratoryTestType(id, "Hemograma", "Hemograma completo", CLINICA, false, CREADO,
                null, true);
    }

    public static LaboratoryTestType propioDeEmpresaDeshabilitado() {
        return new LaboratoryTestType(TYPE_ID, "Hemograma", "Hemograma completo", CLINICA, false,
                CREADO, null, false);
    }

    /** Tipo general, disponible para todas las empresas: sin company. */
    public static LaboratoryTestType general() {
        return new LaboratoryTestType(TYPE_ID, "Perfil renal", "Perfil renal basico", null, true,
                CREADO, null, true);
    }

    public static CreateLaboratoryTestTypeCommand comandoCrearPropio() {
        return new CreateLaboratoryTestTypeCommand("Hemograma", "Hemograma completo", COMPANY_ID,
                false);
    }

    public static CreateLaboratoryTestTypeCommand comandoCrearGeneral() {
        return new CreateLaboratoryTestTypeCommand("Perfil renal", "Perfil renal basico", null,
                true);
    }

    public static UpdateLaboratoryTestTypeCommand comandoActualizarPropio() {
        return new UpdateLaboratoryTestTypeCommand(TYPE_ID, "Hemograma completo",
                "Hemograma completo con formula", COMPANY_ID, false);
    }
}
