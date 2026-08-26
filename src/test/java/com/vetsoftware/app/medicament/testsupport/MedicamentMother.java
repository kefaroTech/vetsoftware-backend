package com.vetsoftware.app.medicament.testsupport;

import com.vetsoftware.app.medicament.domain.CompanyRef;
import com.vetsoftware.app.medicament.domain.Medicament;
import java.time.LocalDateTime;

/**
 * Fixtures del modulo medicament.
 *
 * <p>
 * Las instancias con id se construyen con el constructor publico y no con
 * {@code Medicament.create(...)}: el factory deja el id en {@code null} y pone
 * {@code LocalDateTime.now()}, asi que no sirve para representar una FILA ya
 * persistida ni permite afirmar sobre {@code createdDate}.
 *
 * <p>
 * El par activo/pausado del mismo nombre es lo que ejercita la guarda de #559:
 * la pausa LIBERA el nombre —el indice unico cubre solo las filas activas—, asi
 * que el alta que se encuentra la pausada la reactiva, y la que se encuentra la
 * activa choca.
 *
 * <p>
 * <b>NO uses los fixtures GLOBALES de esta mother en un test que toque la base
 * real</b> —{@code general()}, {@code activoGeneral()} y
 * {@code pausadoGeneral()}—. Su nombre, «Amoxicilina», coincide con una de las
 * 154 moleculas que siembra el changeset
 * {@code 299_seed_global_medicament_catalog.xml}, y la comparacion la hace la
 * base con {@code utf8mb4_0900_ai_ci}: insensible a acentos y a caja. Una fila
 * global comparte {@code owner_scope = 0} con la semilla, asi que el INSERT
 * chocaria contra {@code uq_medicaments_owner_active_name} y el test fallaria
 * al montarse, con un error que apunta a la constraint y no al descuido.
 *
 * <p>
 * Los fixtures de EMPRESA no tienen ese problema —otro {@code owner_scope}— y
 * aqui nada molesta, porque estos fixtures solo alimentan dobles que nunca
 * llegan al motor. Para una rodaja de persistencia, usa un nombre sintetico que
 * la semilla no contenga, como hace {@code MedicamentPersistenceIT}.
 */
public final class MedicamentMother {

    public static final Long MEDICAMENT_ID = 70L;
    public static final Long COMPANY_ID = 9L;
    public static final Long OTRA_COMPANY_ID = 99L;

    public static final CompanyRef CLINICA = new CompanyRef(COMPANY_ID, "Clinica Norte",
            "900123456");

    /** Fecha fija: {@code create()} usa {@code now()} y no seria afirmable. */
    public static final LocalDateTime CREADO = LocalDateTime.of(2026, 1, 15, 10, 30);

    private MedicamentMother() {
    }

    public static CompanyRef companyRef() {
        return CLINICA;
    }

    public static Medicament general() {
        return Medicament.create("Amoxicilina", "Antibiotico de amplio espectro", null, true);
    }

    public static Medicament propioDeEmpresa(CompanyRef company) {
        return Medicament.create("Suero especial", "Formula propia", company, false);
    }

    /** Fila de empresa ACTIVA que ocupa el nombre «Suero». */
    public static Medicament activoDeEmpresa() {
        return new Medicament(MEDICAMENT_ID, "Suero", "Formula propia", CLINICA, false, CREADO, 0L,
                true);
    }

    /** La misma fila PAUSADA: no ocupa el nombre, se reactiva. */
    public static Medicament pausadoDeEmpresa() {
        return new Medicament(MEDICAMENT_ID, "Suero", "Formula de hace dos años", CLINICA, false,
                CREADO, 0L, false);
    }

    /** Fila del vademecum de plataforma ACTIVA ({@code company_id IS NULL}). */
    public static Medicament activoGeneral() {
        return new Medicament(MEDICAMENT_ID, "Amoxicilina", "Antibiotico", null, true, CREADO, 0L,
                true);
    }

    /** La misma fila global, PAUSADA. */
    public static Medicament pausadoGeneral() {
        return new Medicament(MEDICAMENT_ID, "Amoxicilina", "Antibiotico retirado", null, true,
                CREADO, 0L, false);
    }
}
