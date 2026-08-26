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
 *
 * <p>
 * <b>NO uses los fixtures GLOBALES de esta mother en un test que toque la base
 * real</b> —{@code general()}, {@code generalDeshabilitado()},
 * {@code comandoCrearGeneral()} y
 * {@code comandoCrearGeneralConDescripcionNueva()}—. Su nombre, «Perfil renal»,
 * coincide con una fila de la semilla del changeset
 * {@code 296_seed_laboratory_test_types_catalog.xml} bajo la collation
 * {@code utf8mb4_0900_ai_ci}, que es insensible a acentos y a caja. Una fila
 * global comparte {@code owner_scope = 0} con la semilla, asi que el INSERT
 * chocaria contra {@code uq_laboratory_test_types_owner_active_name} y el test
 * fallaria al montarse, con un error que apunta a la constraint y no al
 * descuido. Quitar la tilde no es escapatoria: bajo esa collation da igual.
 *
 * <p>
 * Los fixtures de EMPRESA no tienen ese problema —otro {@code owner_scope}— y
 * aqui nada molesta, porque estos fixtures solo alimentan dobles que nunca
 * llegan al motor. Para una rodaja de persistencia, construye el fixture con un
 * nombre propio con sufijo de prueba, como hace
 * {@code LaboratoryTestTypePersistenceIT}.
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

    /**
     * Tipo GENERAL dado de baja: el que ocupa un nombre del catalogo de plataforma.
     */
    public static LaboratoryTestType generalDeshabilitado() {
        return new LaboratoryTestType(TYPE_ID, "Perfil renal", "Perfil renal basico", null, true,
                CREADO, null, false);
    }

    public static CreateLaboratoryTestTypeCommand comandoCrearPropio() {
        return new CreateLaboratoryTestTypeCommand("Hemograma", "Hemograma completo", COMPANY_ID,
                false);
    }

    public static CreateLaboratoryTestTypeCommand comandoCrearGeneral() {
        return new CreateLaboratoryTestTypeCommand("Perfil renal", "Perfil renal basico", null,
                true);
    }

    /**
     * Descripcion distinta de la que llevan las filas de arriba: es lo que deja ver
     * que la reactivacion REESCRIBE los detalles y no se limita a subir
     * {@code enabled}.
     */
    public static final String DESCRIPCION_NUEVA = "Hemograma con recuento de plaquetas";

    public static final String DESCRIPCION_GENERAL_NUEVA = "Perfil renal ampliado";

    /**
     * Alta que reutiliza el nombre de la fila dada de baja, con descripcion nueva.
     */
    public static CreateLaboratoryTestTypeCommand comandoCrearPropioConDescripcionNueva() {
        return new CreateLaboratoryTestTypeCommand("Hemograma", DESCRIPCION_NUEVA, COMPANY_ID,
                false);
    }

    public static CreateLaboratoryTestTypeCommand comandoCrearGeneralConDescripcionNueva() {
        return new CreateLaboratoryTestTypeCommand("Perfil renal", DESCRIPCION_GENERAL_NUEVA, null,
                true);
    }

    /**
     * Alta incoherente: declara empresa Y {@code general = true} a la vez. El XOR
     * del dominio la rechaza, y por eso el {@code update} va antes del UPDATE
     * nativo de reactivacion.
     */
    public static CreateLaboratoryTestTypeCommand comandoCrearIncoherente() {
        return new CreateLaboratoryTestTypeCommand("Hemograma", DESCRIPCION_NUEVA, COMPANY_ID,
                true);
    }

    public static UpdateLaboratoryTestTypeCommand comandoActualizarPropio() {
        return new UpdateLaboratoryTestTypeCommand(TYPE_ID, "Hemograma completo",
                "Hemograma completo con formula", COMPANY_ID, false);
    }

    /**
     * Edicion por el camino SYSTEM: sin empresa, sobre el catalogo de plataforma.
     */
    public static UpdateLaboratoryTestTypeCommand comandoActualizarGeneral() {
        return new UpdateLaboratoryTestTypeCommand(TYPE_ID, "Perfil renal",
                DESCRIPCION_GENERAL_NUEVA, null, true);
    }
}
