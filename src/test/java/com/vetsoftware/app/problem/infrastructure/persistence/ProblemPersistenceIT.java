package com.vetsoftware.app.problem.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.problem.domain.AnimalRef;
import com.vetsoftware.app.problem.domain.CompanyRef;
import com.vetsoftware.app.problem.domain.Problem;
import com.vetsoftware.app.problem.domain.ProblemStatus;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import com.vetsoftware.app.testsupport.PersistenceSliceConfig;
import com.vetsoftware.app.testsupport.SchemaSeed;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

/**
 * Rodaja de persistencia de problemas clinicos contra MySQL real.
 *
 * <p>
 * {@code ProblemJpaEntity} tiene FKs obligatorias a {@code animals} y
 * {@code companies}, y a su vez {@code animals} exige specie, breed, owner y
 * color propios. {@link SchemaSeed} no siembra un animal, asi que este test
 * completa la cadena con sus propias filas (ids 960+, fuera del rango de
 * {@link SchemaSeed}) para poder guardar un problema real.
 */
@Import(PersistenceSliceConfig.class)
@DisplayName("JpaProblemRepository — problemas clinicos contra MySQL real")
class ProblemPersistenceIT extends AbstractDataJpaTest {

    private static final Long EMPRESA = SchemaSeed.COMPANY_ID;
    private static final Long OTRA_EMPRESA = SchemaSeed.OTRA_COMPANY_ID;

    private static final Long SPECIE_ID = 960L;
    private static final Long BREED_ID = 961L;
    private static final Long OWNER_ID = 962L;
    private static final Long COLOR_ID = 963L;
    private static final Long ANIMAL_ID = 964L;
    private static final Long OTRO_ANIMAL_ID = 965L;

    private static final AnimalRef FIRULAIS = new AnimalRef(ANIMAL_ID, "Firulais", "A-001");
    private static final AnimalRef MICHI = new AnimalRef(OTRO_ANIMAL_ID, "Michi", "A-002");
    private static final CompanyRef CLINICA = new CompanyRef(EMPRESA, "Veterinaria de prueba",
            "900123456");
    private static final CompanyRef CLINICA_AJENA = new CompanyRef(OTRA_EMPRESA,
            "Veterinaria ajena", "900654321");

    private static final LocalDate INICIO = LocalDate.of(2026, 1, 10);
    private static final LocalDateTime CREADO = LocalDateTime.of(2026, 1, 15, 10, 30);

    @Autowired
    private JpaProblemRepository repository;

    @PersistenceContext
    private EntityManager entityManager;

    @BeforeEach
    void sembrarLasFilasRaiz() {
        SchemaSeed.seed(entityManager);
        entityManager.createNativeQuery("""
                INSERT IGNORE INTO species (id, name, created_date, enabled)
                VALUES (:id, 'Perro-PB', '2026-01-01 00:00:00', true)
                """).setParameter("id", SPECIE_ID).executeUpdate();
        entityManager.createNativeQuery("""
                INSERT IGNORE INTO breeds (id, name, specie_id, created_date, enabled)
                VALUES (:id, 'Labrador-PB', :specie, '2026-01-01 00:00:00', true)
                """).setParameter("id", BREED_ID).setParameter("specie", SPECIE_ID).executeUpdate();
        entityManager.createNativeQuery("""
                INSERT IGNORE INTO animal_colors (id, name, specie_id, created_date, enabled)
                VALUES (:id, 'Negro-PB', :specie, '2026-01-01 00:00:00', true)
                """).setParameter("id", COLOR_ID).setParameter("specie", SPECIE_ID).executeUpdate();
        entityManager.createNativeQuery("""
                INSERT IGNORE INTO owners (id, name, document, document_type, person_type,
                                           withholding_agent, tax_regime, fiscal_responsibility,
                                           city_id, company_id, created_date, enabled)
                VALUES (:id, 'Ana Ruiz', 'CC-PB-1', 'CEDULA_CIUDADANIA', 'NATURAL', false,
                        'NO_RESPONSABLE_IVA', 'NO_APLICA', :ciudad, :empresa,
                        '2026-01-01 00:00:00', true)
                """).setParameter("id", OWNER_ID).setParameter("ciudad", SchemaSeed.CITY_ID)
                .setParameter("empresa", EMPRESA).executeUpdate();
        animal(ANIMAL_ID, "Firulais", "A-001", EMPRESA);
        animal(OTRO_ANIMAL_ID, "Michi", "A-002", EMPRESA);
        entityManager.flush();
    }

    private void animal(Long id, String nombre, String codigo, Long companyId) {
        entityManager.createNativeQuery("""
                INSERT IGNORE INTO animals (id, name, code, specie_id, breed_id, owner_id, gender,
                                            weight_type, animal_type, reproductive_state, color_id,
                                            deceased, company_id, created_date, enabled)
                VALUES (:id, :nombre, :codigo, :specie, :breed, :owner, 'MALE', 'KILOGRAMS', 'NONE',
                        'STERILIZED', :color, false, :empresa, '2026-01-01 00:00:00', true)
                """).setParameter("id", id).setParameter("nombre", nombre)
                .setParameter("codigo", codigo).setParameter("specie", SPECIE_ID)
                .setParameter("breed", BREED_ID).setParameter("owner", OWNER_ID)
                .setParameter("color", COLOR_ID).setParameter("empresa", companyId).executeUpdate();
    }

    private void releerDesdeLaBase() {
        entityManager.flush();
        entityManager.clear();
    }

    private Problem nuevoProblema(AnimalRef animal, CompanyRef empresa, String descripcion,
            ProblemStatus estado, LocalDateTime creado) {
        return new Problem(null, animal, empresa, descripcion, estado, INICIO, null,
                "Revisar en dos semanas", creado, null, true);
    }

    @Nested
    @DisplayName("guardar y releer")
    class GuardarYReleer {

        @Test
        @DisplayName("guarda el problema y lo relee con animal y empresa resueltos")
        void guarda_el_problema_y_lo_relee() {
            Problem guardado = repository.save(nuevoProblema(FIRULAIS, CLINICA,
                    "Dermatitis alergica", ProblemStatus.ACTIVE, CREADO));
            releerDesdeLaBase();

            Problem leido = repository.findByIdAndCompanyId(guardado.getId(), EMPRESA)
                    .orElseThrow();

            assertThat(leido.getAnimal()).isEqualTo(FIRULAIS);
            assertThat(leido.getCompany()).isEqualTo(CLINICA);
            assertThat(leido.getDescription()).isEqualTo("Dermatitis alergica");
            assertThat(leido.getStatus()).isEqualTo(ProblemStatus.ACTIVE);
            assertThat(leido.getOnsetDate()).isEqualTo(INICIO);
            assertThat(leido.getResolvedDate()).isNull();
            assertThat(leido.getNotes()).isEqualTo("Revisar en dos semanas");
            assertThat(leido.getCreatedDate()).isEqualTo(CREADO);
            assertThat(leido.isEnabled()).isTrue();
        }
    }

    @Nested
    @DisplayName("estado")
    class Estado {

        @ParameterizedTest
        @EnumSource(ProblemStatus.class)
        @DisplayName("cada estado se relee tal cual desde la columna VARCHAR")
        void cada_estado_se_relee_tal_cual(ProblemStatus estado) {
            // @Enumerated(STRING): si alguien lo pasara a ORDINAL, las filas ya escritas
            // se releerian como el estado equivocado.
            Problem guardado = repository
                    .save(nuevoProblema(FIRULAIS, CLINICA, "Dermatitis alergica", estado, CREADO));
            releerDesdeLaBase();

            assertThat(repository.findByIdAndCompanyId(guardado.getId(), EMPRESA))
                    .map(Problem::getStatus).contains(estado);
        }
    }

    @Nested
    @DisplayName("aislamiento por empresa")
    class Tenancy {

        @Test
        @DisplayName("un problema de otra empresa no se encuentra por id")
        void un_problema_de_otra_empresa_no_se_encuentra_por_id() {
            Problem ajeno = repository.save(
                    nuevoProblema(MICHI, CLINICA_AJENA, "Otitis", ProblemStatus.ACTIVE, CREADO));
            releerDesdeLaBase();

            assertThat(repository.findByIdAndCompanyId(ajeno.getId(), EMPRESA)).isEmpty();
        }

        @Test
        @DisplayName("el listado por animal no mezcla problemas de otro tenant")
        void el_listado_por_animal_no_mezcla_problemas_de_otro_tenant() {
            Problem propio = repository.save(
                    nuevoProblema(FIRULAIS, CLINICA, "Dermatitis", ProblemStatus.ACTIVE, CREADO));
            repository.save(
                    nuevoProblema(MICHI, CLINICA_AJENA, "Otitis", ProblemStatus.ACTIVE, CREADO));
            releerDesdeLaBase();

            PageResult<Problem> pagina = repository.findByAnimalIdAndCompanyId(ANIMAL_ID, EMPRESA,
                    0, 20);

            assertThat(pagina.content()).extracting(Problem::getId).containsExactly(propio.getId());
        }
    }

    @Nested
    @DisplayName("listado por animal")
    class ListadoPorAnimal {

        @Test
        @DisplayName("ordena por fecha de creacion descendente")
        void ordena_por_fecha_de_creacion_descendente() {
            Problem antiguo = repository.save(nuevoProblema(FIRULAIS, CLINICA, "Antiguo",
                    ProblemStatus.ACTIVE, LocalDateTime.of(2026, 1, 1, 8, 0)));
            Problem reciente = repository.save(nuevoProblema(FIRULAIS, CLINICA, "Reciente",
                    ProblemStatus.ACTIVE, LocalDateTime.of(2026, 2, 1, 8, 0)));
            releerDesdeLaBase();

            PageResult<Problem> pagina = repository.findByAnimalIdAndCompanyId(ANIMAL_ID, EMPRESA,
                    0, 20);

            assertThat(pagina.content()).extracting(Problem::getId)
                    .containsExactly(reciente.getId(), antiguo.getId());
        }

        @Test
        @DisplayName("dos paginas consecutivas ni repiten ni omiten filas")
        void dos_paginas_consecutivas_ni_repiten_ni_omiten_filas() {
            Problem uno = repository.save(nuevoProblema(FIRULAIS, CLINICA, "Uno",
                    ProblemStatus.ACTIVE, LocalDateTime.of(2026, 1, 1, 8, 0)));
            Problem dos = repository.save(nuevoProblema(FIRULAIS, CLINICA, "Dos",
                    ProblemStatus.ACTIVE, LocalDateTime.of(2026, 1, 2, 8, 0)));
            Problem tres = repository.save(nuevoProblema(FIRULAIS, CLINICA, "Tres",
                    ProblemStatus.ACTIVE, LocalDateTime.of(2026, 1, 3, 8, 0)));
            releerDesdeLaBase();

            PageResult<Problem> pagina0 = repository.findByAnimalIdAndCompanyId(ANIMAL_ID, EMPRESA,
                    0, 2);
            PageResult<Problem> pagina1 = repository.findByAnimalIdAndCompanyId(ANIMAL_ID, EMPRESA,
                    1, 2);

            assertThat(pagina0.content()).extracting(Problem::getId).containsExactly(tres.getId(),
                    dos.getId());
            assertThat(pagina1.content()).extracting(Problem::getId).containsExactly(uno.getId());
            assertThat(pagina0.totalElements()).isEqualTo(3L);
            assertThat(pagina0.totalPages()).isEqualTo(2);
        }

        @Test
        @DisplayName("un animal sin problemas devuelve una pagina vacia")
        void un_animal_sin_problemas_devuelve_pagina_vacia() {
            PageResult<Problem> pagina = repository.findByAnimalIdAndCompanyId(OTRO_ANIMAL_ID,
                    EMPRESA, 0, 20);

            assertThat(pagina.content()).isEmpty();
            assertThat(pagina.totalElements()).isZero();
        }
    }

    @Nested
    @DisplayName("borrado")
    class Borrado {

        @Test
        @DisplayName("el borrado es logico: la fila deja de verse por id y en el listado")
        void el_borrado_es_logico_y_la_fila_deja_de_verse() {
            // @SQLDelete la marca enabled = false y @SQLRestriction la esconde: si alguna
            // de las dos se cayera, o se perderia el historial clinico o seguirian
            // apareciendo problemas ya cerrados.
            Problem guardado = repository.save(
                    nuevoProblema(FIRULAIS, CLINICA, "Dermatitis", ProblemStatus.ACTIVE, CREADO));
            releerDesdeLaBase();

            repository.delete(guardado.getId(), EMPRESA);
            releerDesdeLaBase();

            assertThat(repository.findByIdAndCompanyId(guardado.getId(), EMPRESA)).isEmpty();
            assertThat(repository.findByAnimalIdAndCompanyId(ANIMAL_ID, EMPRESA, 0, 20).content())
                    .isEmpty();
        }

        @Test
        @DisplayName("borrar un problema de otra empresa no hace nada")
        void borrar_un_problema_de_otra_empresa_no_hace_nada() {
            Problem guardado = repository.save(
                    nuevoProblema(FIRULAIS, CLINICA, "Dermatitis", ProblemStatus.ACTIVE, CREADO));
            releerDesdeLaBase();

            repository.delete(guardado.getId(), OTRA_EMPRESA);
            releerDesdeLaBase();

            assertThat(repository.findByIdAndCompanyId(guardado.getId(), EMPRESA)).isPresent();
        }
    }
}
