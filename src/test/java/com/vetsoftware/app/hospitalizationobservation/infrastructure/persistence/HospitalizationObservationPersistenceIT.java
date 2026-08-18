package com.vetsoftware.app.hospitalizationobservation.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.hospitalization.domain.AnimalRef;
import com.vetsoftware.app.hospitalization.domain.CompanyRef;
import com.vetsoftware.app.hospitalization.domain.Hospitalization;
import com.vetsoftware.app.hospitalization.domain.HospitalizationType;
import com.vetsoftware.app.hospitalization.infrastructure.persistence.HospitalizationJpaMapper;
import com.vetsoftware.app.hospitalization.infrastructure.persistence.JpaHospitalizationRepository;
import com.vetsoftware.app.hospitalizationobservation.domain.EmployeeRef;
import com.vetsoftware.app.hospitalizationobservation.domain.HospitalizationObservation;
import com.vetsoftware.app.hospitalizationobservation.domain.HospitalizationRef;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import com.vetsoftware.app.testsupport.SchemaSeed;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

/**
 * Rodaja de persistencia de las observaciones de hospitalizacion contra MySQL
 * real (BE-COV). Cubre en un solo test los tres adaptadores JPA de la feature:
 * {@link JpaHospitalizationObservationRepository},
 * {@link JpaHospitalizationQueryPort} y {@link JpaEmployeeQueryPort}.
 *
 * <p>
 * <b>Por que un doble no sirve aqui.</b>
 *
 * <ul>
 * <li>El {@code @EntityGraph} de
 * {@code HospitalizationObservationJpaRepository} es lo que evita el N+1 al
 * leer {@code hospitalization} y {@code createdBy}: solo se ve pasando por
 * Hibernate.</li>
 * <li>El orden por id descendente de
 * {@code findAllByHospitalizationIdAndCompanyId} es SQL real (un {@code Sort}
 * mal armado no lo ve un mock).</li>
 * <li>El soft delete ({@code @SQLDelete}/{@code @SQLRestriction}) y el
 * {@code reactivate} nativo solo se comprueban contra el schema real.</li>
 * </ul>
 *
 * <p>
 * La hospitalizacion padre se crea con el repositorio real de esa feature
 * (importado aqui) en vez de por SQL nativo: lo que se prueba es la FK contra
 * una fila valida, no el camino de escritura de esa feature vecina.
 */
@Import({JpaHospitalizationObservationRepository.class, HospitalizationObservationJpaMapper.class,
        JpaHospitalizationQueryPort.class, JpaEmployeeQueryPort.class,
        JpaHospitalizationRepository.class, HospitalizationJpaMapper.class})
@DisplayName("Adaptadores JPA de hospitalizationobservation contra MySQL real")
class HospitalizationObservationPersistenceIT extends AbstractDataJpaTest {

    private static final Long COMPANY_ID = SchemaSeed.COMPANY_ID;
    private static final Long OTRA_COMPANY_ID = SchemaSeed.OTRA_COMPANY_ID;
    private static final Long EMPLOYEE_ID = SchemaSeed.EMPLOYEE_ID;

    private static final Long OWNER_ID = 990L;
    private static final Long SPECIE_ID = 991L;
    private static final Long BREED_ID = 992L;
    private static final Long COLOR_ID = 993L;
    private static final Long ANIMAL_ID = 994L;

    @Autowired
    private JpaHospitalizationObservationRepository repository;
    @Autowired
    private JpaHospitalizationQueryPort hospitalizationQueryPort;
    @Autowired
    private JpaEmployeeQueryPort employeeQueryPort;
    @Autowired
    private JpaHospitalizationRepository hospitalizationRepository;

    @PersistenceContext
    private EntityManager entityManager;

    private Long hospitalizacionId;
    private HospitalizationRef hospitalizacion;

    @BeforeEach
    void sembrarLasFilasRaiz() {
        SchemaSeed.seed(entityManager);
        propietario();
        catalogoDeAnimal();
        animal();
        entityManager.flush();

        Hospitalization h = Hospitalization.create(LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 1), null, HospitalizationType.HOSPITALIZATION, null,
                "Motivo de la hospitalizacion", null,
                new AnimalRef(ANIMAL_ID, "Firulais-HO", "A-HO-001"), null,
                new CompanyRef(COMPANY_ID, "Veterinaria de prueba", "900123456"));
        Hospitalization guardada = hospitalizationRepository.save(h);
        entityManager.flush();
        entityManager.clear();
        hospitalizacionId = guardada.getId();
        hospitalizacion = new HospitalizationRef(hospitalizacionId, guardada.getDate());
    }

    private void propietario() {
        entityManager.createNativeQuery("""
                INSERT IGNORE INTO owners (id, name, document, document_type, person_type,
                                           withholding_agent, tax_regime, fiscal_responsibility,
                                           city_id, company_id, created_date, enabled)
                VALUES (:id, 'Marta Diaz-HO', 'CC-HO-1', 'CEDULA_CIUDADANIA', 'NATURAL', false,
                        'NO_RESPONSABLE_IVA', 'NO_APLICA', :ciudad, :empresa,
                        '2026-01-01 08:00:00', true)
                """).setParameter("id", OWNER_ID).setParameter("ciudad", SchemaSeed.CITY_ID)
                .setParameter("empresa", COMPANY_ID).executeUpdate();
    }

    private void catalogoDeAnimal() {
        entityManager.createNativeQuery("""
                INSERT IGNORE INTO species (id, name, created_date, enabled)
                VALUES (:id, 'Canino-HO', '2026-01-01 08:00:00', true)
                """).setParameter("id", SPECIE_ID).executeUpdate();
        entityManager.createNativeQuery("""
                INSERT IGNORE INTO breeds (id, name, specie_id, created_date, enabled)
                VALUES (:id, 'Criollo-HO', :specie, '2026-01-01 08:00:00', true)
                """).setParameter("id", BREED_ID).setParameter("specie", SPECIE_ID).executeUpdate();
        entityManager.createNativeQuery("""
                INSERT IGNORE INTO animal_colors (id, name, specie_id, created_date, enabled)
                VALUES (:id, 'Negro-HO', :specie, '2026-01-01 08:00:00', true)
                """).setParameter("id", COLOR_ID).setParameter("specie", SPECIE_ID).executeUpdate();
    }

    private void animal() {
        entityManager
                .createNativeQuery(
                        """
                                INSERT IGNORE INTO animals (id, name, code, specie_id, breed_id, owner_id, gender,
                                                            weight_type, animal_type, reproductive_state, color_id,
                                                            deceased, company_id, created_date, enabled)
                                VALUES (:id, 'Firulais-HO', 'A-HO-001', :specie, :breed, :owner, 'MALE', 'KILOGRAMS',
                                        'NONE', 'UNKNOWN', :color, false, :empresa, '2026-01-01 08:00:00', true)
                                """)
                .setParameter("id", ANIMAL_ID).setParameter("specie", SPECIE_ID)
                .setParameter("breed", BREED_ID).setParameter("owner", OWNER_ID)
                .setParameter("color", COLOR_ID).setParameter("empresa", COMPANY_ID)
                .executeUpdate();
    }

    private HospitalizationObservation observacionNueva(EmployeeRef createdBy) {
        return HospitalizationObservation.create("Paciente estable, sin novedades", hospitalizacion,
                createdBy);
    }

    @Nested
    @DisplayName("JpaHospitalizationObservationRepository — ida y vuelta")
    class IdaYVuelta {

        @Test
        @DisplayName("guardar asigna id y releer hidrata hospitalizacion y creador")
        void guardar_asigna_id_y_releer_hidrata_las_asociaciones() {
            EmployeeRef veterinario = employeeQueryPort.findById(EMPLOYEE_ID).orElseThrow();

            HospitalizationObservation guardada = repository.save(observacionNueva(veterinario));
            entityManager.flush();
            entityManager.clear();

            assertThat(guardada.getId()).isNotNull();
            HospitalizationObservation leida = repository.findById(guardada.getId()).orElseThrow();
            assertThat(leida.getHospitalization().id()).isEqualTo(hospitalizacionId);
            assertThat(leida.getCreatedBy()).isEqualTo(veterinario);
            assertThat(leida.getDescription()).isEqualTo("Paciente estable, sin novedades");
            assertThat(leida.isEnabled()).isTrue();
        }
    }

    @Nested
    @DisplayName("JpaHospitalizationObservationRepository — aislamiento por empresa")
    class AislamientoPorEmpresa {

        @Test
        @DisplayName("findByIdAndCompanyId no cruza empresas: el filtro va por la hospitalizacion")
        void find_by_id_and_company_id_filtra_por_empresa() {
            EmployeeRef veterinario = employeeQueryPort.findById(EMPLOYEE_ID).orElseThrow();
            HospitalizationObservation guardada = repository.save(observacionNueva(veterinario));
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findByIdAndCompanyId(guardada.getId(), OTRA_COMPANY_ID))
                    .isEmpty();
            assertThat(repository.findByIdAndCompanyId(guardada.getId(), COMPANY_ID)).isPresent();
        }
    }

    @Nested
    @DisplayName("JpaHospitalizationObservationRepository — listado paginado por hospitalizacion")
    class Listado {

        @Test
        @DisplayName("pagina con lo mas reciente primero y filtra por empresa")
        void pagina_con_lo_mas_reciente_primero() {
            EmployeeRef veterinario = employeeQueryPort.findById(EMPLOYEE_ID).orElseThrow();
            HospitalizationObservation primera = repository.save(observacionNueva(veterinario));
            HospitalizationObservation segunda = repository.save(observacionNueva(veterinario));
            entityManager.flush();
            entityManager.clear();

            PageResult<HospitalizationObservation> pagina = repository
                    .findAllByHospitalizationIdAndCompanyId(hospitalizacionId, COMPANY_ID, 0, 20);

            assertThat(pagina.content()).extracting(HospitalizationObservation::getId)
                    .containsExactly(segunda.getId(), primera.getId());
            assertThat(pagina.totalElements()).isEqualTo(2);
        }

        @Test
        @DisplayName("una hospitalizacion sin observaciones de esa empresa recibe una pagina vacia")
        void hospitalizacion_sin_observaciones_de_esa_empresa_recibe_pagina_vacia() {
            PageResult<HospitalizationObservation> pagina = repository
                    .findAllByHospitalizationIdAndCompanyId(hospitalizacionId, OTRA_COMPANY_ID, 0,
                            20);

            assertThat(pagina.content()).isEmpty();
            assertThat(pagina.totalElements()).isZero();
        }
    }

    @Nested
    @DisplayName("JpaHospitalizationObservationRepository — baja y reactivacion")
    class BajaYReactivacion {

        @Test
        @DisplayName("eliminar aplica soft delete y reactivate la revive")
        void eliminar_y_reactivar() {
            EmployeeRef veterinario = employeeQueryPort.findById(EMPLOYEE_ID).orElseThrow();
            HospitalizationObservation guardada = repository.save(observacionNueva(veterinario));
            entityManager.flush();
            entityManager.clear();

            repository.delete(guardada.getId());
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findById(guardada.getId())).isEmpty();

            int actualizadas = repository.reactivate(guardada.getId(), COMPANY_ID);
            entityManager.clear();

            assertThat(actualizadas).isOne();
            assertThat(repository.findById(guardada.getId())).isPresent();
        }

        @Test
        @DisplayName("reactivate sobre un id inexistente no afecta ninguna fila")
        void reactivate_sobre_id_inexistente_no_afecta_filas() {
            assertThat(repository.reactivate(999_999L, COMPANY_ID)).isZero();
        }

        /**
         * El caso que el SQL tiene que rechazar. La empresa no cuelga de la observacion
         * sino de la hospitalizacion padre, asi que lo que se ejercita aqui es el
         * {@code EXISTS} del UPDATE nativo: con el companyId de otra empresa afecta
         * cero filas y la observacion sigue deshabilitada.
         */
        @Test
        @DisplayName("reactivate() con el companyId de otra empresa no reactiva nada y la fila sigue deshabilitada")
        void reactivate_con_otra_empresa_no_afecta_filas() {
            EmployeeRef veterinario = employeeQueryPort.findById(EMPLOYEE_ID).orElseThrow();
            HospitalizationObservation guardada = repository.save(observacionNueva(veterinario));
            entityManager.flush();
            entityManager.clear();

            repository.delete(guardada.getId());
            entityManager.flush();
            entityManager.clear();

            int actualizadas = repository.reactivate(guardada.getId(), OTRA_COMPANY_ID);
            entityManager.clear();

            assertThat(actualizadas).isZero();
            assertThat(repository.findById(guardada.getId())).isEmpty();
        }
    }

    @Nested
    @DisplayName("JpaHospitalizationQueryPort")
    class HospitalizationQuery {

        @Test
        @DisplayName("findById mapea la hospitalizacion a su ref con la fecha real")
        void find_by_id_mapea_la_hospitalizacion() {
            Optional<HospitalizationRef> ref = hospitalizationQueryPort.findById(hospitalizacionId);

            assertThat(ref)
                    .contains(new HospitalizationRef(hospitalizacionId, LocalDate.of(2026, 3, 1)));
        }

        @Test
        @DisplayName("una hospitalizacion inexistente devuelve vacio")
        void hospitalizacion_inexistente_devuelve_vacio() {
            assertThat(hospitalizationQueryPort.findById(999_999L)).isEmpty();
        }
    }

    @Nested
    @DisplayName("JpaEmployeeQueryPort")
    class EmployeeQuery {

        @Test
        @DisplayName("findById mapea el empleado a su ref con codigo y nombre")
        void find_by_id_mapea_el_empleado() {
            Optional<EmployeeRef> ref = employeeQueryPort.findById(EMPLOYEE_ID);

            assertThat(ref).isPresent();
            assertThat(ref.get().id()).isEqualTo(EMPLOYEE_ID);
            assertThat(ref.get().employeeCode()).isEqualTo("EMP-001");
        }

        @Test
        @DisplayName("un empleado inexistente devuelve vacio")
        void empleado_inexistente_devuelve_vacio() {
            assertThat(employeeQueryPort.findById(999_999L)).isEmpty();
        }
    }
}
