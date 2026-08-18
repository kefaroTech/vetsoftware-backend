package com.vetsoftware.app.procedureschedule.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.procedureschedule.domain.AppliedStatus;
import com.vetsoftware.app.procedureschedule.domain.EmployeeRef;
import com.vetsoftware.app.procedureschedule.domain.HospitalizationProcedureRef;
import com.vetsoftware.app.procedureschedule.domain.ProcedureSchedule;
import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import com.vetsoftware.app.testsupport.SchemaSeed;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

/**
 * Rodaja de persistencia del plan de ejecuciones de un procedimiento contra
 * MySQL real (BE-10).
 *
 * <p>
 * La cadena de FK es larga: propietario, catalogo de animal, animal,
 * hospitalizacion y orden de procedimiento, todas sembradas por SQL nativo — lo
 * que se prueba es la tabla {@code procedure_schedules}, no el camino de
 * escritura de esas features padre.
 */
@Import({JpaProcedureScheduleRepository.class, ProcedureScheduleJpaMapper.class})
@DisplayName("JpaProcedureScheduleRepository — plan de ejecuciones contra MySQL real")
class ProcedureSchedulePersistenceIT extends AbstractDataJpaTest {

    private static final Long OWNER = 980L;
    private static final Long SPECIE = 981L;
    private static final Long BREED = 982L;
    private static final Long COLOR = 983L;
    private static final Long ANIMAL = 984L;
    private static final Long HOSPITALIZATION = 985L;
    private static final Long PROCEDURE = 986L;

    /** Cadena paralela colgada de {@link SchemaSeed#OTRA_COMPANY_ID}. */
    private static final Long ANIMAL_AJENO = 1094L;
    private static final Long HOSPITALIZATION_AJENA = 1095L;
    private static final Long PROCEDURE_AJENO = 1096L;

    private static final HospitalizationProcedureRef ORDEN_AJENA = new HospitalizationProcedureRef(
            PROCEDURE_AJENO, "Drenaje de absceso");

    private static final HospitalizationProcedureRef ORDEN = new HospitalizationProcedureRef(
            PROCEDURE, "Curacion de herida");
    private static final EmployeeRef EMPLEADO = new EmployeeRef(SchemaSeed.EMPLOYEE_ID, "EMP-001",
            "Ana Ruiz");

    private static final LocalDateTime PRIMERA_TOMA = LocalDateTime.of(2026, 1, 15, 8, 0);

    @Autowired
    private JpaProcedureScheduleRepository repository;

    @PersistenceContext
    private EntityManager entityManager;

    @BeforeEach
    void sembrarLasFilasRaiz() {
        SchemaSeed.seed(entityManager);
        propietario();
        catalogoDeAnimal();
        animal();
        hospitalizacion();
        procedimiento();
        cadenaDeLaEmpresaAjena();
        entityManager.flush();
    }

    /**
     * Un animal, una hospitalizacion y un procedimiento identicos pero colgados de
     * OTRA empresa. Es lo que hace comprobables los filtros: el {@code EXISTS} del
     * UPDATE y el JOIN de los finders tienen que subir dos saltos hasta
     * {@code hospitalizations.company_id} para distinguirlos.
     */
    private void cadenaDeLaEmpresaAjena() {
        entityManager.createNativeQuery("""
                INSERT IGNORE INTO animals (id, name, code, specie_id, breed_id, owner_id, gender,
                                            weight_type, animal_type, reproductive_state, color_id,
                                            deceased, company_id, created_date, enabled)
                VALUES (:id, 'Michi', 'A-012', :specie, :breed, :owner, 'FEMALE', 'KILOGRAMS',
                        'NONE', 'UNKNOWN', :color, false, :empresa, '2026-01-01 08:00:00', true)
                """).setParameter("id", ANIMAL_AJENO).setParameter("specie", SPECIE)
                .setParameter("breed", BREED).setParameter("owner", OWNER)
                .setParameter("color", COLOR).setParameter("empresa", SchemaSeed.OTRA_COMPANY_ID)
                .executeUpdate();
        entityManager.createNativeQuery("""
                INSERT IGNORE INTO hospitalizations (id, date, start_date, end_date, type, reason,
                                                     animal_id, company_id, created_date, enabled)
                VALUES (:id, '2026-01-15', '2026-01-15', '2026-01-20', 'HOSPITALIZATION',
                        'Absceso', :animal, :empresa, '2026-01-15 08:00:00', true)
                """).setParameter("id", HOSPITALIZATION_AJENA).setParameter("animal", ANIMAL_AJENO)
                .setParameter("empresa", SchemaSeed.OTRA_COMPANY_ID).executeUpdate();
        entityManager.createNativeQuery("""
                INSERT IGNORE INTO hospitalization_procedures (id, name, frequency, guideline_type,
                                                                duration_measure, duration_quantity,
                                                                start_date, start_time,
                                                                hospitalization_id, created_by_id,
                                                                created_date, enabled)
                VALUES (:id, 'Drenaje de absceso', 'EVERY_8H', 'FIXED', 'DAYS', 3, '2026-01-15',
                        '08:00:00', :hosp, :empleado, '2026-01-15 07:30:00', true)
                """).setParameter("id", PROCEDURE_AJENO).setParameter("hosp", HOSPITALIZATION_AJENA)
                .setParameter("empleado", SchemaSeed.EMPLOYEE_ID).executeUpdate();
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
                .setParameter("empresa", SchemaSeed.COMPANY_ID).executeUpdate();
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
                .setParameter("color", COLOR).setParameter("empresa", SchemaSeed.COMPANY_ID)
                .executeUpdate();
    }

    private void hospitalizacion() {
        entityManager.createNativeQuery("""
                INSERT IGNORE INTO hospitalizations (id, date, start_date, end_date, type, reason,
                                                     animal_id, company_id, created_date, enabled)
                VALUES (:id, '2026-01-15', '2026-01-15', '2026-01-20', 'HOSPITALIZATION',
                        'Gastroenteritis aguda', :animal, :empresa, '2026-01-15 08:00:00', true)
                """).setParameter("id", HOSPITALIZATION).setParameter("animal", ANIMAL)
                .setParameter("empresa", SchemaSeed.COMPANY_ID).executeUpdate();
    }

    private void procedimiento() {
        entityManager.createNativeQuery("""
                INSERT IGNORE INTO hospitalization_procedures (id, name, frequency, guideline_type,
                                                                duration_measure, duration_quantity,
                                                                start_date, start_time,
                                                                hospitalization_id, created_by_id,
                                                                created_date, enabled)
                VALUES (:id, 'Curacion de herida', 'EVERY_8H', 'FIXED', 'DAYS', 3, '2026-01-15',
                        '08:00:00', :hosp, :empleado, '2026-01-15 07:30:00', true)
                """).setParameter("id", PROCEDURE).setParameter("hosp", HOSPITALIZATION)
                .setParameter("empleado", SchemaSeed.EMPLOYEE_ID).executeUpdate();
    }

    private ProcedureSchedule guardar(LocalDateTime cuando) {
        ProcedureSchedule guardada = repository.save(ProcedureSchedule.create(ORDEN, cuando, cuando,
                AppliedStatus.PENDING, false, EMPLEADO));
        entityManager.flush();
        entityManager.clear();
        return guardada;
    }

    @Nested
    @DisplayName("Escritura y lectura")
    class Escritura {

        @Test
        @DisplayName("guarda una toma pendiente y la devuelve al releer, con sus refs resueltas")
        void guarda_y_relee_una_toma_pendiente() {
            ProcedureSchedule guardada = guardar(PRIMERA_TOMA);

            assertThat(guardada.getId()).isNotNull();
            ProcedureSchedule releida = repository.findById(guardada.getId()).orElseThrow();
            assertThat(releida.getHospitalizationProcedure()).isEqualTo(ORDEN);
            assertThat(releida.getCreatedBy()).isEqualTo(EMPLEADO);
            assertThat(releida.getAppliedStatus()).isEqualTo(AppliedStatus.PENDING);
            assertThat(releida.getCurrentDateTime()).isEqualTo(PRIMERA_TOMA);
        }

        @Test
        @DisplayName("un id inexistente no se encuentra")
        void un_id_inexistente_no_se_encuentra() {
            assertThat(repository.findById(999999L)).isEmpty();
        }
    }

    @Nested
    @DisplayName("Listados")
    class Listados {

        @Test
        @DisplayName("findByHospitalizationProcedureId trae todas las tomas de la orden")
        void find_by_hospitalization_procedure_id_trae_todas_las_tomas() {
            guardar(PRIMERA_TOMA);
            guardar(PRIMERA_TOMA.plusHours(8));

            assertThat(repository.findByHospitalizationProcedureId(PROCEDURE)).hasSize(2);
        }

        @Test
        @DisplayName("findByHospitalizationId trae las tomas de toda la hospitalizacion")
        void find_by_hospitalization_id_trae_las_tomas_de_la_hospitalizacion() {
            guardar(PRIMERA_TOMA);

            assertThat(repository.findByHospitalizationId(HOSPITALIZATION)).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Baja y reactivacion")
    class BajaYReactivacion {

        @Test
        @DisplayName("delete borra la fila de forma definitiva, no un soft delete")
        void delete_borra_la_fila_de_forma_definitiva() {
            Long id = guardar(PRIMERA_TOMA).getId();

            repository.delete(id);
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findById(id)).isEmpty();
        }

        @Test
        @DisplayName("disableByHospitalizationProcedureId deshabilita todas las tomas de la orden")
        void disable_by_hospitalization_procedure_id_deshabilita_todas() {
            guardar(PRIMERA_TOMA);
            guardar(PRIMERA_TOMA.plusHours(8));

            repository.disableByHospitalizationProcedureId(PROCEDURE);
            entityManager.clear();

            assertThat(repository.findByHospitalizationProcedureId(PROCEDURE)).isEmpty();
        }

        @Test
        @DisplayName("disablePendingByHospitalizationProcedureId conserva las ya aplicadas")
        void disable_pending_conserva_las_aplicadas() {
            guardar(PRIMERA_TOMA.plusHours(8));
            ProcedureSchedule aplicada = repository.save(ProcedureSchedule.create(ORDEN,
                    PRIMERA_TOMA, PRIMERA_TOMA, AppliedStatus.APPLIED, false, EMPLEADO));
            entityManager.flush();
            entityManager.clear();

            repository.disablePendingByHospitalizationProcedureId(PROCEDURE);
            entityManager.clear();

            assertThat(repository.findByHospitalizationProcedureId(PROCEDURE))
                    .extracting(ProcedureSchedule::getId).containsExactly(aplicada.getId());
        }

    }

    @Nested
    @DisplayName("Tenancy")
    class Tenancy {

        private ProcedureSchedule guardarAjena(LocalDateTime cuando) {
            ProcedureSchedule guardada = repository.save(ProcedureSchedule.create(ORDEN_AJENA,
                    cuando, cuando, AppliedStatus.PENDING, false, EMPLEADO));
            entityManager.flush();
            entityManager.clear();
            return guardada;
        }

        @Test
        @DisplayName("findByHospitalizationProcedureIdAndCompanyId no ve la orden de otra empresa")
        void find_by_procedure_acotado_no_ve_la_orden_ajena() {
            guardarAjena(PRIMERA_TOMA);

            assertThat(repository.findByHospitalizationProcedureIdAndCompanyId(PROCEDURE_AJENO,
                    SchemaSeed.COMPANY_ID)).isEmpty();
            assertThat(repository.findByHospitalizationProcedureIdAndCompanyId(PROCEDURE_AJENO,
                    SchemaSeed.OTRA_COMPANY_ID)).hasSize(1);
        }

        @Test
        @DisplayName("findByHospitalizationIdAndCompanyId no ve la hospitalizacion de otra empresa")
        void find_by_hospitalization_acotado_no_ve_la_hospitalizacion_ajena() {
            guardarAjena(PRIMERA_TOMA);

            assertThat(repository.findByHospitalizationIdAndCompanyId(HOSPITALIZATION_AJENA,
                    SchemaSeed.COMPANY_ID)).isEmpty();
            assertThat(repository.findByHospitalizationIdAndCompanyId(HOSPITALIZATION_AJENA,
                    SchemaSeed.OTRA_COMPANY_ID)).hasSize(1);
        }

        /**
         * El {@code EXISTS} contra {@code hospitalizations} es toda la seguridad de
         * este UPDATE: no hay lectura previa que valide la propiedad. Con la empresa
         * equivocada no puede tocar ni una fila.
         */
        @Test
        @DisplayName("disableByHospitalizationProcedureId acotado no toca las ejecuciones ajenas")
        void disable_acotado_no_toca_las_ejecuciones_ajenas() {
            guardarAjena(PRIMERA_TOMA);
            guardarAjena(PRIMERA_TOMA.plusHours(8));

            repository.disableByHospitalizationProcedureId(PROCEDURE_AJENO, SchemaSeed.COMPANY_ID);
            entityManager.clear();

            assertThat(repository.findByHospitalizationProcedureId(PROCEDURE_AJENO)).hasSize(2);
        }

        @Test
        @DisplayName("disablePendingByHospitalizationProcedureId acotado no toca las ajenas")
        void disable_pending_acotado_no_toca_las_ejecuciones_ajenas() {
            guardarAjena(PRIMERA_TOMA);

            repository.disablePendingByHospitalizationProcedureId(PROCEDURE_AJENO,
                    SchemaSeed.COMPANY_ID);
            entityManager.clear();

            assertThat(repository.findByHospitalizationProcedureId(PROCEDURE_AJENO)).hasSize(1);
        }

        @Test
        @DisplayName("con su propia empresa el UPDATE acotado si deshabilita")
        void con_su_empresa_el_update_acotado_si_deshabilita() {
            guardarAjena(PRIMERA_TOMA);

            repository.disableByHospitalizationProcedureId(PROCEDURE_AJENO,
                    SchemaSeed.OTRA_COMPANY_ID);
            entityManager.clear();

            assertThat(repository.findByHospitalizationProcedureId(PROCEDURE_AJENO)).isEmpty();
        }
    }
}
