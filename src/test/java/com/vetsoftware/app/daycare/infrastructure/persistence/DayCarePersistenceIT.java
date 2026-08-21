package com.vetsoftware.app.daycare.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.daycare.domain.AnimalRef;
import com.vetsoftware.app.daycare.domain.CompanyRef;
import com.vetsoftware.app.daycare.domain.DayCare;
import com.vetsoftware.app.daycare.domain.DayCareType;
import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import com.vetsoftware.app.testsupport.PersistenceSliceConfig;
import com.vetsoftware.app.testsupport.SchemaSeed;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

/**
 * Rodaja de persistencia de guarderia/hotel contra MySQL real.
 *
 * <p>
 * {@code DayCareJpaEntity} tiene FKs obligatorias a {@code animals} y
 * {@code companies}, y a su vez {@code animals} exige specie, breed, owner y
 * color propios. {@link SchemaSeed} no siembra un animal, asi que este test
 * completa la cadena con sus propias filas (ids 970+, fuera del rango de
 * {@link SchemaSeed} y del resto de features que ya usan el rango 960-969).
 */
@Import(PersistenceSliceConfig.class)
@DisplayName("JpaDayCareRepository — guarderia/hotel contra MySQL real")
class DayCarePersistenceIT extends AbstractDataJpaTest {

    private static final Long EMPRESA = SchemaSeed.COMPANY_ID;
    private static final Long OTRA_EMPRESA = SchemaSeed.OTRA_COMPANY_ID;

    private static final Long SPECIE_ID = 970L;
    private static final Long BREED_ID = 971L;
    private static final Long OWNER_ID = 972L;
    private static final Long COLOR_ID = 973L;
    private static final Long ANIMAL_ID = 974L;
    private static final Long OTRO_ANIMAL_ID = 975L;

    private static final AnimalRef FIRULAIS = new AnimalRef(ANIMAL_ID, "Firulais-DC", "A-DC-001");
    private static final AnimalRef MICHI = new AnimalRef(OTRO_ANIMAL_ID, "Michi-DC", "A-DC-002");
    private static final CompanyRef CLINICA = new CompanyRef(EMPRESA, "Veterinaria de prueba",
            "900123456");

    @Autowired
    private JpaDayCareRepository repository;

    @PersistenceContext
    private EntityManager entityManager;

    @BeforeEach
    void sembrarLasFilasRaiz() {
        SchemaSeed.seed(entityManager);
        entityManager.createNativeQuery("""
                INSERT IGNORE INTO species (id, name, created_date, enabled)
                VALUES (:id, 'Perro-DC', '2026-01-01 00:00:00', true)
                """).setParameter("id", SPECIE_ID).executeUpdate();
        entityManager.createNativeQuery("""
                INSERT IGNORE INTO breeds (id, name, specie_id, created_date, enabled)
                VALUES (:id, 'Labrador-DC', :specie, '2026-01-01 00:00:00', true)
                """).setParameter("id", BREED_ID).setParameter("specie", SPECIE_ID).executeUpdate();
        entityManager.createNativeQuery("""
                INSERT IGNORE INTO animal_colors (id, name, specie_id, created_date, enabled)
                VALUES (:id, 'Negro-DC', :specie, '2026-01-01 00:00:00', true)
                """).setParameter("id", COLOR_ID).setParameter("specie", SPECIE_ID).executeUpdate();
        entityManager.createNativeQuery("""
                INSERT IGNORE INTO owners (id, name, document, document_type, person_type,
                                           withholding_agent, tax_regime, fiscal_responsibility,
                                           city_id, company_id, created_date, enabled)
                VALUES (:id, 'Ana Ruiz', 'CC-DC-1', 'CEDULA_CIUDADANIA', 'NATURAL', false,
                        'NO_RESPONSABLE_IVA', 'NO_APLICA', :ciudad, :empresa,
                        '2026-01-01 00:00:00', true)
                """).setParameter("id", OWNER_ID).setParameter("ciudad", SchemaSeed.CITY_ID)
                .setParameter("empresa", EMPRESA).executeUpdate();
        animal(ANIMAL_ID, "Firulais-DC", "A-DC-001");
        animal(OTRO_ANIMAL_ID, "Michi-DC", "A-DC-002");
        entityManager.flush();
    }

    private void animal(Long id, String nombre, String codigo) {
        entityManager.createNativeQuery("""
                INSERT IGNORE INTO animals (id, name, code, specie_id, breed_id, owner_id, gender,
                                            weight_type, animal_type, reproductive_state, color_id,
                                            deceased, company_id, created_date, enabled)
                VALUES (:id, :nombre, :codigo, :specie, :breed, :owner, 'MALE', 'KILOGRAMS', 'NONE',
                        'STERILIZED', :color, false, :empresa, '2026-01-01 00:00:00', true)
                """).setParameter("id", id).setParameter("nombre", nombre)
                .setParameter("codigo", codigo).setParameter("specie", SPECIE_ID)
                .setParameter("breed", BREED_ID).setParameter("owner", OWNER_ID)
                .setParameter("color", COLOR_ID).setParameter("empresa", EMPRESA).executeUpdate();
    }

    private DayCare nuevoDayCare(AnimalRef animal, DayCareType tipo) {
        return DayCare.create(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 1),
                LocalDate.of(2026, 2, 3), tipo, "Correa, plato", "Sin novedades", animal, CLINICA);
    }

    @Nested
    @DisplayName("guardar y releer")
    class GuardarYReleer {

        @Test
        @DisplayName("guarda el daycare y lo relee con animal y empresa resueltos")
        void guarda_y_relee_con_animal_y_empresa_resueltos() {
            DayCare guardado = repository.save(nuevoDayCare(FIRULAIS, DayCareType.DAYCARE));
            entityManager.flush();
            entityManager.clear();

            Optional<DayCare> releido = repository.findById(guardado.getId());

            assertThat(releido).isPresent();
            assertThat(releido.get().getAnimal()).isEqualTo(FIRULAIS);
            assertThat(releido.get().getCompany()).isEqualTo(CLINICA);
            assertThat(releido.get().getType()).isEqualTo(DayCareType.DAYCARE);
            assertThat(releido.get().getObjects()).isEqualTo("Correa, plato");
            assertThat(releido.get().isEnabled()).isTrue();
        }
    }

    @Nested
    @DisplayName("findByIdAndCompanyId — aislamiento por empresa")
    class AislamientoPorEmpresa {

        @Test
        @DisplayName("un daycare no se lee con la empresa equivocada")
        void un_daycare_no_se_lee_con_la_empresa_equivocada() {
            DayCare guardado = repository.save(nuevoDayCare(FIRULAIS, DayCareType.HOTEL));
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findByIdAndCompanyId(guardado.getId(), OTRA_EMPRESA)).isEmpty();
            assertThat(repository.findByIdAndCompanyId(guardado.getId(), EMPRESA)).isPresent();
        }
    }

    @Nested
    @DisplayName("findAllByAnimalIdAndCompanyId — paginado y filtro de texto")
    class ListadoPorAnimal {

        @Test
        @DisplayName("devuelve solo los daycares del animal pedido, mas recientes primero")
        void devuelve_solo_los_daycares_del_animal_ordenados_por_id_desc() {
            DayCare primero = repository.save(nuevoDayCare(FIRULAIS, DayCareType.DAYCARE));
            DayCare segundo = repository.save(nuevoDayCare(FIRULAIS, DayCareType.HOTEL));
            repository.save(nuevoDayCare(MICHI, DayCareType.DAYCARE));
            entityManager.flush();
            entityManager.clear();

            var pagina = repository.findAllByAnimalIdAndCompanyId(ANIMAL_ID, EMPRESA, null, 0, 20);

            assertThat(pagina.content()).extracting(DayCare::getId).containsExactly(segundo.getId(),
                    primero.getId());
        }

        @Test
        @DisplayName("el filtro de texto busca en objects y observations")
        void el_filtro_de_texto_busca_en_objects_y_observations() {
            repository.save(nuevoDayCare(FIRULAIS, DayCareType.DAYCARE));
            entityManager.flush();
            entityManager.clear();

            var conFiltro = repository.findAllByAnimalIdAndCompanyId(ANIMAL_ID, EMPRESA, "plato", 0,
                    20);
            var sinCoincidencia = repository.findAllByAnimalIdAndCompanyId(ANIMAL_ID, EMPRESA,
                    "inexistente", 0, 20);

            assertThat(conFiltro.content()).hasSize(1);
            assertThat(sinCoincidencia.content()).isEmpty();
        }

        @Test
        @DisplayName("un animal sin daycares recibe una pagina vacia")
        void un_animal_sin_daycares_recibe_pagina_vacia() {
            var pagina = repository.findAllByAnimalIdAndCompanyId(OTRO_ANIMAL_ID, EMPRESA, null, 0,
                    20);

            assertThat(pagina.content()).isEmpty();
            assertThat(pagina.totalElements()).isZero();
        }
    }

    @Nested
    @DisplayName("findAll — listado global")
    class ListadoGlobal {

        @Test
        @DisplayName("incluye daycares de todas las empresas")
        void incluye_daycares_de_todas_las_empresas() {
            DayCare deEstaEmpresa = repository.save(nuevoDayCare(FIRULAIS, DayCareType.DAYCARE));
            entityManager.flush();
            entityManager.clear();

            List<DayCare> todos = repository.findAll();

            assertThat(todos).extracting(DayCare::getId).contains(deEstaEmpresa.getId());
        }
    }

    @Nested
    @DisplayName("borrado logico y reactivacion")
    class BorradoYReactivacion {

        @Test
        @DisplayName("delete deshabilita la fila: deja de aparecer por id")
        void delete_deshabilita_la_fila() {
            DayCare guardado = repository.save(nuevoDayCare(FIRULAIS, DayCareType.DAYCARE));
            entityManager.flush();
            entityManager.clear();

            repository.delete(guardado.getId());
            entityManager.flush();
            entityManager.clear();

            // @SQLDelete + @SQLRestriction("enabled = true"): la fila sigue en la tabla
            // pero ninguna lectura de la entidad la vuelve a traer.
            assertThat(repository.findById(guardado.getId())).isEmpty();
        }

        @Test
        @DisplayName("reactivate vuelve a habilitar la fila borrada")
        void reactivate_vuelve_a_habilitar_la_fila() {
            DayCare guardado = repository.save(nuevoDayCare(FIRULAIS, DayCareType.DAYCARE));
            entityManager.flush();
            repository.delete(guardado.getId());
            entityManager.flush();
            entityManager.clear();

            int filas = repository.reactivate(guardado.getId(), EMPRESA);
            entityManager.clear();

            assertThat(filas).isEqualTo(1);
            assertThat(repository.findByIdAndCompanyId(guardado.getId(), EMPRESA)).isPresent();
        }

        @Test
        @DisplayName("reactivate con el companyId de OTRA empresa no afecta ninguna fila")
        void reactivate_con_empresa_ajena_no_afecta_filas() {
            DayCare guardado = repository.save(nuevoDayCare(FIRULAIS, DayCareType.DAYCARE));
            entityManager.flush();
            repository.delete(guardado.getId());
            entityManager.flush();
            entityManager.clear();

            int filas = repository.reactivate(guardado.getId(), OTRA_EMPRESA);
            entityManager.clear();

            assertThat(filas).isZero();
            assertThat(repository.findById(guardado.getId())).isEmpty();
        }

        @Test
        @DisplayName("reactivate sobre un id inexistente no afecta ninguna fila")
        void reactivate_sobre_id_inexistente_no_afecta_filas() {
            assertThat(repository.reactivate(999_999L, EMPRESA)).isZero();
        }
    }
}
