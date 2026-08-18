package com.vetsoftware.app.hospitalizationprocedure.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.hospitalization.domain.AnimalRef;
import com.vetsoftware.app.hospitalization.domain.CompanyRef;
import com.vetsoftware.app.hospitalization.domain.Hospitalization;
import com.vetsoftware.app.hospitalization.domain.HospitalizationType;
import com.vetsoftware.app.hospitalization.infrastructure.persistence.HospitalizationJpaMapper;
import com.vetsoftware.app.hospitalization.infrastructure.persistence.JpaHospitalizationRepository;
import com.vetsoftware.app.hospitalizationprocedure.domain.DurationMeasure;
import com.vetsoftware.app.hospitalizationprocedure.domain.EmployeeRef;
import com.vetsoftware.app.hospitalizationprocedure.domain.Frequency;
import com.vetsoftware.app.hospitalizationprocedure.domain.GuidelineType;
import com.vetsoftware.app.hospitalizationprocedure.domain.HospitalizationProcedure;
import com.vetsoftware.app.hospitalizationprocedure.domain.HospitalizationRef;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import com.vetsoftware.app.testsupport.SchemaSeed;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

/**
 * Rodaja de persistencia de las ordenes de procedimiento contra MySQL real
 * (BE-10).
 *
 * <p>
 * Lo que no ve un test de mapper ni de service: el {@code @EntityGraph} que
 * hidrata {@code hospitalization}, {@code createdBy} y {@code suspensionBy} en
 * una sola consulta, el orden por id descendente que resuelve
 * {@code findAllByHospitalizationIdAndCompanyId}, y que el soft delete
 * ({@code @SQLDelete}/{@code @SQLRestriction}) y el {@code reactivate} nativo
 * funcionan contra el schema real.
 *
 * <p>
 * La hospitalizacion padre se crea con el repositorio real de esa feature
 * (importado aqui igual que en {@code HospitalizationPersistenceIT}) en vez de
 * por SQL nativo: lo que se prueba es la FK contra una fila valida, no el
 * camino de escritura de esa feature vecina.
 */
@Import({JpaHospitalizationProcedureRepository.class, HospitalizationProcedureJpaMapper.class,
        JpaHospitalizationRepository.class, HospitalizationJpaMapper.class})
@DisplayName("JpaHospitalizationProcedureRepository — ordenes de procedimiento contra MySQL real")
class HospitalizationProcedurePersistenceIT extends AbstractDataJpaTest {

    private static final Long EMPRESA = SchemaSeed.COMPANY_ID;
    private static final Long OTRA_EMPRESA = SchemaSeed.OTRA_COMPANY_ID;
    private static final Long EMPLEADO = SchemaSeed.EMPLOYEE_ID;
    private static final Long OTRO_EMPLEADO = SchemaSeed.OTRO_EMPLOYEE_ID;

    /** Padres propios de esta rodaja (ids 970+, fuera del rango de SchemaSeed). */
    private static final Long OWNER = 970L;
    private static final Long SPECIE = 971L;
    private static final Long BREED = 972L;
    private static final Long COLOR = 973L;
    private static final Long ANIMAL = 974L;

    private static final EmployeeRef CREADO_POR = new EmployeeRef(EMPLEADO, "EMP-001", "Ana Ruiz");
    private static final EmployeeRef SUSPENDIDO_POR = new EmployeeRef(OTRO_EMPLEADO, "EMP-002",
            "Luis Paz");

    @Autowired
    private JpaHospitalizationProcedureRepository repository;
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
                LocalDate.of(2026, 3, 1), null, HospitalizationType.HOSPITALIZATION, null, "Motivo",
                null, new AnimalRef(ANIMAL, "Firulais", "A-001"), null,
                new CompanyRef(EMPRESA, "Veterinaria de prueba", "900123456"));
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
                VALUES (:id, 'Marta Diaz', 'CC-1001', 'CEDULA_CIUDADANIA', 'NATURAL', false,
                        'NO_RESPONSABLE_IVA', 'NO_APLICA', :ciudad, :empresa,
                        '2026-01-01 08:00:00', true)
                """).setParameter("id", OWNER).setParameter("ciudad", SchemaSeed.CITY_ID)
                .setParameter("empresa", EMPRESA).executeUpdate();
    }

    private void catalogoDeAnimal() {
        entityManager.createNativeQuery("""
                INSERT IGNORE INTO species (id, name, created_date, enabled)
                VALUES (:id, 'Canino', '2026-01-01 08:00:00', true)
                """).setParameter("id", SPECIE).executeUpdate();
        entityManager.createNativeQuery("""
                INSERT IGNORE INTO breeds (id, name, specie_id, created_date, enabled)
                VALUES (:id, 'Criollo', :specie, '2026-01-01 08:00:00', true)
                """).setParameter("id", BREED).setParameter("specie", SPECIE).executeUpdate();
        entityManager.createNativeQuery("""
                INSERT IGNORE INTO animal_colors (id, name, specie_id, created_date, enabled)
                VALUES (:id, 'Negro', :specie, '2026-01-01 08:00:00', true)
                """).setParameter("id", COLOR).setParameter("specie", SPECIE).executeUpdate();
    }

    private void animal() {
        entityManager.createNativeQuery("""
                INSERT IGNORE INTO animals (id, name, code, specie_id, breed_id, owner_id, gender,
                                            weight_type, animal_type, reproductive_state, color_id,
                                            deceased, company_id, created_date, enabled)
                VALUES (:id, 'Firulais', 'A-001', :specie, :breed, :owner, 'MALE', 'KILOGRAMS',
                        'NONE', 'UNKNOWN', :color, false, :empresa, '2026-01-01 08:00:00', true)
                """).setParameter("id", ANIMAL).setParameter("specie", SPECIE)
                .setParameter("breed", BREED).setParameter("owner", OWNER)
                .setParameter("color", COLOR).setParameter("empresa", EMPRESA).executeUpdate();
    }

    private HospitalizationProcedure orden(EmployeeRef creadoPor) {
        return HospitalizationProcedure.create("Curacion de herida", "Solucion salina 0.9%",
                Frequency.EVERY_8H, GuidelineType.INTERVAL, DurationMeasure.DAYS, 5,
                LocalDate.of(2026, 3, 1), LocalTime.of(8, 0), "Notas", hospitalizacion, creadoPor);
    }

    @Nested
    @DisplayName("ida y vuelta")
    class IdaYVuelta {

        @Test
        @DisplayName("guardar asigna id y releer hidrata hospitalizacion y creador")
        void guardar_asigna_id_y_releer_hidrata_las_asociaciones() {
            HospitalizationProcedure guardado = repository.save(orden(CREADO_POR));
            entityManager.flush();
            entityManager.clear();

            assertThat(guardado.getId()).isNotNull();
            HospitalizationProcedure leido = repository.findById(guardado.getId()).orElseThrow();
            assertThat(leido.getHospitalization().id()).isEqualTo(hospitalizacionId);
            assertThat(leido.getCreatedBy()).isEqualTo(CREADO_POR);
            assertThat(leido.getName()).isEqualTo("Curacion de herida");
            assertThat(leido.getFrequency()).isEqualTo(Frequency.EVERY_8H);
            assertThat(leido.getGuidelineType()).isEqualTo(GuidelineType.INTERVAL);
            assertThat(leido.getDurationMeasure()).isEqualTo(DurationMeasure.DAYS);
            assertThat(leido.getSuspensionBy()).isNull();
        }

        @Test
        @DisplayName("findByIdAndCompanyId no cruza empresas: el filtro va por la hospitalizacion")
        void find_by_id_and_company_id_filtra_por_empresa() {
            HospitalizationProcedure guardado = repository.save(orden(CREADO_POR));
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findByIdAndCompanyId(guardado.getId(), EMPRESA)).isPresent();
            assertThat(repository.findByIdAndCompanyId(guardado.getId(), OTRA_EMPRESA)).isEmpty();
        }
    }

    @Nested
    @DisplayName("suspension persistida")
    class Suspension {

        @Test
        @DisplayName("guardar con suspension hidrata tambien el suspensionBy al releer")
        void guardar_con_suspension_hidrata_suspension_by() {
            HospitalizationProcedure creado = repository.save(orden(CREADO_POR));
            entityManager.flush();
            entityManager.clear();

            HospitalizationProcedure recargado = repository.findById(creado.getId()).orElseThrow();
            recargado.suspend(SUSPENDIDO_POR, LocalDateTime.of(2026, 3, 2, 9, 0));
            repository.save(recargado);
            entityManager.flush();
            entityManager.clear();

            HospitalizationProcedure leido = repository.findById(creado.getId()).orElseThrow();
            assertThat(leido.getSuspensionBy()).isEqualTo(SUSPENDIDO_POR);
            assertThat(leido.getSuspensionDate()).isEqualTo(LocalDateTime.of(2026, 3, 2, 9, 0));
        }
    }

    @Nested
    @DisplayName("listado paginado por hospitalizacion")
    class Listado {

        @Test
        @DisplayName("pagina con lo mas reciente primero y filtra por empresa")
        void pagina_con_lo_mas_reciente_primero() {
            HospitalizationProcedure primero = repository.save(orden(CREADO_POR));
            HospitalizationProcedure segundo = repository.save(orden(CREADO_POR));
            entityManager.flush();
            entityManager.clear();

            PageResult<HospitalizationProcedure> pagina = repository
                    .findAllByHospitalizationIdAndCompanyId(hospitalizacionId, EMPRESA, 0, 20);

            assertThat(pagina.content()).extracting(HospitalizationProcedure::getId)
                    .containsExactly(segundo.getId(), primero.getId());
            assertThat(pagina.totalElements()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("baja y reactivacion")
    class BajaYReactivacion {

        @Test
        @DisplayName("eliminar aplica soft delete y reactivate la revive")
        void eliminar_y_reactivar() {
            HospitalizationProcedure guardado = repository.save(orden(CREADO_POR));
            entityManager.flush();
            entityManager.clear();

            repository.delete(guardado.getId());
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findById(guardado.getId())).isEmpty();

            int actualizadas = repository.reactivate(guardado.getId(), EMPRESA);
            entityManager.clear();

            assertThat(actualizadas).isOne();
            assertThat(repository.findById(guardado.getId())).isPresent();
        }

        @Test
        @DisplayName("reactivate() sobre un id inexistente no afecta filas")
        void reactivate_sobre_id_inexistente() {
            assertThat(repository.reactivate(999_999L, EMPRESA)).isZero();
        }

        /**
         * El caso que justifica el arreglo: la empresa no cuelga de
         * {@code hospitalization_procedures} sino de la hospitalizacion padre, asi que
         * el UPDATE nativo la alcanza por un {@code EXISTS} contra
         * {@code hospitalizations}. Antes el UPDATE solo filtraba por id y reactivaba
         * la orden de cualquier tenant que conociera el id — y aqui no hay lectura
         * previa que lo impida.
         */
        @Test
        @DisplayName("reactivate() con el companyId de otra empresa no reactiva nada y la fila sigue deshabilitada")
        void reactivate_con_company_id_de_otra_empresa_no_reactiva_nada() {
            HospitalizationProcedure guardado = repository.save(orden(CREADO_POR));
            entityManager.flush();
            entityManager.clear();
            repository.delete(guardado.getId());
            entityManager.flush();
            entityManager.clear();

            int actualizadas = repository.reactivate(guardado.getId(), OTRA_EMPRESA);
            entityManager.clear();

            assertThat(actualizadas).isZero();
            assertThat(repository.findById(guardado.getId())).isEmpty();
        }
    }
}
