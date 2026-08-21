package com.vetsoftware.app.spa.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.spa.domain.AnimalRef;
import com.vetsoftware.app.spa.domain.CompanyRef;
import com.vetsoftware.app.spa.domain.Spa;
import com.vetsoftware.app.spa.domain.SpaStatus;
import com.vetsoftware.app.spa.domain.SpaTypeRef;
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
 * Rodaja de persistencia de citas de spa contra MySQL real.
 *
 * <p>
 * {@code SpaJpaEntity} tiene FKs obligatorias a {@code spa_types},
 * {@code animals} y {@code companies}, y a su vez {@code animals} exige specie,
 * breed, owner y color propios. {@link SchemaSeed} no siembra un animal ni un
 * tipo de spa, asi que este test completa la cadena con sus propias filas (ids
 * 980+, fuera del rango de {@link SchemaSeed} y del resto de features que ya
 * usan el rango 960-979).
 */
@Import(PersistenceSliceConfig.class)
@DisplayName("JpaSpaRepository — citas de spa contra MySQL real")
class SpaPersistenceIT extends AbstractDataJpaTest {

    private static final Long EMPRESA = SchemaSeed.COMPANY_ID;
    private static final Long OTRA_EMPRESA = SchemaSeed.OTRA_COMPANY_ID;

    private static final Long SPECIE_ID = 980L;
    private static final Long BREED_ID = 981L;
    private static final Long OWNER_ID = 982L;
    private static final Long COLOR_ID = 983L;
    private static final Long ANIMAL_ID = 984L;
    private static final Long OTRO_ANIMAL_ID = 985L;
    private static final Long SPA_TYPE_ID = 986L;

    private static final AnimalRef FIRULAIS = new AnimalRef(ANIMAL_ID, "Firulais-SP", "A-SP-001");
    private static final AnimalRef MICHI = new AnimalRef(OTRO_ANIMAL_ID, "Michi-SP", "A-SP-002");
    private static final CompanyRef CLINICA = new CompanyRef(EMPRESA, "Veterinaria de prueba",
            "900123456");
    private static final SpaTypeRef BANO_BASICO = new SpaTypeRef(SPA_TYPE_ID, "Baño básico-SP");

    @Autowired
    private JpaSpaRepository repository;

    @PersistenceContext
    private EntityManager entityManager;

    @BeforeEach
    void sembrarLasFilasRaiz() {
        SchemaSeed.seed(entityManager);
        entityManager.createNativeQuery("""
                INSERT IGNORE INTO species (id, name, created_date, enabled)
                VALUES (:id, 'Perro-SP', '2026-01-01 00:00:00', true)
                """).setParameter("id", SPECIE_ID).executeUpdate();
        entityManager.createNativeQuery("""
                INSERT IGNORE INTO breeds (id, name, specie_id, created_date, enabled)
                VALUES (:id, 'Labrador-SP', :specie, '2026-01-01 00:00:00', true)
                """).setParameter("id", BREED_ID).setParameter("specie", SPECIE_ID).executeUpdate();
        entityManager.createNativeQuery("""
                INSERT IGNORE INTO animal_colors (id, name, specie_id, created_date, enabled)
                VALUES (:id, 'Negro-SP', :specie, '2026-01-01 00:00:00', true)
                """).setParameter("id", COLOR_ID).setParameter("specie", SPECIE_ID).executeUpdate();
        entityManager.createNativeQuery("""
                INSERT IGNORE INTO owners (id, name, document, document_type, person_type,
                                           withholding_agent, tax_regime, fiscal_responsibility,
                                           city_id, company_id, created_date, enabled)
                VALUES (:id, 'Ana Ruiz', 'CC-SP-1', 'CEDULA_CIUDADANIA', 'NATURAL', false,
                        'NO_RESPONSABLE_IVA', 'NO_APLICA', :ciudad, :empresa,
                        '2026-01-01 00:00:00', true)
                """).setParameter("id", OWNER_ID).setParameter("ciudad", SchemaSeed.CITY_ID)
                .setParameter("empresa", EMPRESA).executeUpdate();
        entityManager.createNativeQuery("""
                INSERT IGNORE INTO spa_types (id, name, description, created_date, enabled)
                VALUES (:id, 'Baño básico-SP', 'Descripcion de prueba', '2026-01-01 00:00:00',
                        true)
                """).setParameter("id", SPA_TYPE_ID).executeUpdate();
        animal(ANIMAL_ID, "Firulais-SP", "A-SP-001");
        animal(OTRO_ANIMAL_ID, "Michi-SP", "A-SP-002");
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

    private Spa nuevoSpa(AnimalRef animal) {
        return Spa.create(LocalDate.of(2026, 2, 1), BANO_BASICO, "Baño mensual",
                "Shampoo hipoalergenico", "Sin novedades", animal, CLINICA);
    }

    @Nested
    @DisplayName("guardar y releer")
    class GuardarYReleer {

        @Test
        @DisplayName("guarda el spa y lo relee con tipo, animal y empresa resueltos")
        void guarda_y_relee_con_las_referencias_resueltas() {
            Spa guardado = repository.save(nuevoSpa(FIRULAIS));
            entityManager.flush();
            entityManager.clear();

            Optional<Spa> releido = repository.findById(guardado.getId());

            assertThat(releido).isPresent();
            assertThat(releido.get().getSpaType()).isEqualTo(BANO_BASICO);
            assertThat(releido.get().getAnimal()).isEqualTo(FIRULAIS);
            assertThat(releido.get().getCompany()).isEqualTo(CLINICA);
            assertThat(releido.get().getStatus()).isEqualTo(SpaStatus.AGENDADA);
            assertThat(releido.get().getReason()).isEqualTo("Baño mensual");
            assertThat(releido.get().isEnabled()).isTrue();
        }

        @Test
        @DisplayName("un cambio de estado se conserva tras releer")
        void un_cambio_de_estado_se_conserva_tras_releer() {
            Spa guardado = repository.save(nuevoSpa(FIRULAIS));
            guardado.changeStatus(SpaStatus.COMPLETADO);
            repository.save(guardado);
            entityManager.flush();
            entityManager.clear();

            Optional<Spa> releido = repository.findById(guardado.getId());

            assertThat(releido).isPresent();
            assertThat(releido.get().getStatus()).isEqualTo(SpaStatus.COMPLETADO);
        }
    }

    @Nested
    @DisplayName("findByIdAndCompanyId — aislamiento por empresa")
    class AislamientoPorEmpresa {

        @Test
        @DisplayName("un spa no se lee con la empresa equivocada")
        void un_spa_no_se_lee_con_la_empresa_equivocada() {
            Spa guardado = repository.save(nuevoSpa(FIRULAIS));
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
        @DisplayName("devuelve solo las citas del animal pedido, mas recientes primero")
        void devuelve_solo_las_citas_ordenadas_por_id_desc() {
            Spa primero = repository.save(nuevoSpa(FIRULAIS));
            Spa segundo = repository.save(nuevoSpa(FIRULAIS));
            repository.save(nuevoSpa(MICHI));
            entityManager.flush();
            entityManager.clear();

            var pagina = repository.findAllByAnimalIdAndCompanyId(ANIMAL_ID, EMPRESA, null, 0, 20);

            assertThat(pagina.content()).extracting(Spa::getId).containsExactly(segundo.getId(),
                    primero.getId());
        }

        @Test
        @DisplayName("el filtro de texto busca en reason, details y observations")
        void el_filtro_de_texto_busca_en_reason_details_y_observations() {
            repository.save(nuevoSpa(FIRULAIS));
            entityManager.flush();
            entityManager.clear();

            var conFiltro = repository.findAllByAnimalIdAndCompanyId(ANIMAL_ID, EMPRESA, "shampoo",
                    0, 20);
            var sinCoincidencia = repository.findAllByAnimalIdAndCompanyId(ANIMAL_ID, EMPRESA,
                    "inexistente", 0, 20);

            assertThat(conFiltro.content()).hasSize(1);
            assertThat(sinCoincidencia.content()).isEmpty();
        }

        @Test
        @DisplayName("un animal sin citas recibe una pagina vacia")
        void un_animal_sin_citas_recibe_pagina_vacia() {
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
        @DisplayName("incluye spas de todas las empresas")
        void incluye_spas_de_todas_las_empresas() {
            Spa deEstaEmpresa = repository.save(nuevoSpa(FIRULAIS));
            entityManager.flush();
            entityManager.clear();

            List<Spa> todos = repository.findAll();

            assertThat(todos).extracting(Spa::getId).contains(deEstaEmpresa.getId());
        }
    }

    @Nested
    @DisplayName("borrado logico")
    class BorradoYReactivacion {

        @Test
        @DisplayName("delete deshabilita la fila: deja de aparecer por id")
        void delete_deshabilita_la_fila() {
            Spa guardado = repository.save(nuevoSpa(FIRULAIS));
            entityManager.flush();
            entityManager.clear();

            repository.delete(guardado.getId());
            entityManager.flush();
            entityManager.clear();

            // @SQLDelete + @SQLRestriction("enabled = true"): la fila sigue en la tabla
            // pero ninguna lectura de la entidad la vuelve a traer.
            assertThat(repository.findById(guardado.getId())).isEmpty();
        }
    }
}
