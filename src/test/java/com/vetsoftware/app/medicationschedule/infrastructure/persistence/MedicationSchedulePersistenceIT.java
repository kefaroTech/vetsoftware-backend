package com.vetsoftware.app.medicationschedule.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.medicationschedule.domain.AppliedStatus;
import com.vetsoftware.app.medicationschedule.domain.EmployeeRef;
import com.vetsoftware.app.medicationschedule.domain.HospitalizationMedicationRef;
import com.vetsoftware.app.medicationschedule.domain.MedicationSchedule;
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
 * Rodaja de persistencia del plan de tomas contra MySQL real (BE-10).
 *
 * <p>
 * La cadena de FK es la mas larga de la aplicacion: propietario, catalogo de
 * animal, animal, hospitalizacion y orden de medicacion, todas sembradas por
 * SQL nativo — lo que se prueba es la tabla {@code medication_schedules}, no el
 * camino de escritura de esas features padre.
 */
@Import({JpaMedicationScheduleRepository.class, MedicationScheduleJpaMapper.class})
@DisplayName("JpaMedicationScheduleRepository — plan de tomas contra MySQL real")
class MedicationSchedulePersistenceIT extends AbstractDataJpaTest {

    private static final Long OWNER = 970L;
    private static final Long SPECIE = 971L;
    private static final Long BREED = 972L;
    private static final Long COLOR = 973L;
    private static final Long ANIMAL = 974L;
    private static final Long HOSPITALIZATION = 975L;
    private static final Long MEDICATION = 976L;

    /** Cadena paralela colgada de {@link SchemaSeed#OTRA_COMPANY_ID}. */
    private static final Long ANIMAL_AJENO = 1084L;
    private static final Long HOSPITALIZATION_AJENA = 1085L;
    private static final Long MEDICATION_AJENA = 1086L;

    private static final HospitalizationMedicationRef ORDEN_AJENA = new HospitalizationMedicationRef(
            MEDICATION_AJENA, "Enrofloxacina 50mg");

    private static final HospitalizationMedicationRef ORDEN = new HospitalizationMedicationRef(
            MEDICATION, "Amoxicilina 500mg");
    private static final EmployeeRef EMPLEADO = new EmployeeRef(SchemaSeed.EMPLOYEE_ID, "EMP-001",
            "Ana Ruiz");

    private static final LocalDateTime PRIMERA_TOMA = LocalDateTime.of(2026, 1, 15, 8, 0);

    @Autowired
    private JpaMedicationScheduleRepository repository;

    @PersistenceContext
    private EntityManager entityManager;

    @BeforeEach
    void sembrarLasFilasRaiz() {
        SchemaSeed.seed(entityManager);
        propietario();
        catalogoDeAnimal();
        animal();
        hospitalizacion();
        medicacion();
        cadenaDeLaEmpresaAjena();
        entityManager.flush();
    }

    /**
     * Un animal, una hospitalizacion y una orden de medicacion identicas pero
     * colgadas de OTRA empresa. Es lo que hace comprobables los filtros: el
     * {@code EXISTS} del UPDATE y el JOIN de los finders tienen que subir dos
     * saltos hasta {@code hospitalizations.company_id} para distinguirlas.
     */
    private void cadenaDeLaEmpresaAjena() {
        entityManager.createNativeQuery("""
                INSERT IGNORE INTO animals (id, name, code, specie_id, breed_id, owner_id, gender,
                                            weight_type, animal_type, reproductive_state, color_id,
                                            deceased, company_id, created_date, enabled)
                VALUES (:id, 'Michi', 'A-002', :specie, :breed, :owner, 'FEMALE', 'KILOGRAMS',
                        'NONE', 'UNKNOWN', :color, false, :empresa, '2026-01-01 08:00:00', true)
                """).setParameter("id", ANIMAL_AJENO).setParameter("specie", SPECIE)
                .setParameter("breed", BREED).setParameter("owner", OWNER)
                .setParameter("color", COLOR).setParameter("empresa", SchemaSeed.OTRA_COMPANY_ID)
                .executeUpdate();
        entityManager.createNativeQuery("""
                INSERT IGNORE INTO hospitalizations (id, date, start_date, end_date, type, reason,
                                                     animal_id, company_id, created_date, enabled)
                VALUES (:id, '2026-01-15', '2026-01-15', '2026-01-20', 'HOSPITALIZATION',
                        'Fractura', :animal, :empresa, '2026-01-15 08:00:00', true)
                """).setParameter("id", HOSPITALIZATION_AJENA).setParameter("animal", ANIMAL_AJENO)
                .setParameter("empresa", SchemaSeed.OTRA_COMPANY_ID).executeUpdate();
        entityManager
                .createNativeQuery(
                        """
                                INSERT IGNORE INTO hospitalization_medications (id, name, frequency, guideline_type,
                                                                                 duration_measure, duration_quantity,
                                                                                 start_date, start_time,
                                                                                 hospitalization_id, created_by_id,
                                                                                 created_date, enabled)
                                VALUES (:id, 'Enrofloxacina 50mg', 'EVERY_8H', 'FIXED', 'DAYS', 3, '2026-01-15',
                                        '08:00:00', :hosp, :empleado, '2026-01-15 07:30:00', true)
                                """)
                .setParameter("id", MEDICATION_AJENA).setParameter("hosp", HOSPITALIZATION_AJENA)
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

    private void medicacion() {
        entityManager
                .createNativeQuery(
                        """
                                INSERT IGNORE INTO hospitalization_medications (id, name, frequency, guideline_type,
                                                                                 duration_measure, duration_quantity,
                                                                                 start_date, start_time,
                                                                                 hospitalization_id, created_by_id,
                                                                                 created_date, enabled)
                                VALUES (:id, 'Amoxicilina 500mg', 'EVERY_8H', 'FIXED', 'DAYS', 3, '2026-01-15',
                                        '08:00:00', :hosp, :empleado, '2026-01-15 07:30:00', true)
                                """)
                .setParameter("id", MEDICATION).setParameter("hosp", HOSPITALIZATION)
                .setParameter("empleado", SchemaSeed.EMPLOYEE_ID).executeUpdate();
    }

    private MedicationSchedule guardar(LocalDateTime cuando) {
        MedicationSchedule guardada = repository.save(MedicationSchedule.create(ORDEN, cuando,
                cuando, AppliedStatus.PENDING, false, EMPLEADO));
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
            MedicationSchedule guardada = guardar(PRIMERA_TOMA);

            assertThat(guardada.getId()).isNotNull();
            MedicationSchedule releida = repository.findById(guardada.getId()).orElseThrow();
            assertThat(releida.getHospitalizationMedication()).isEqualTo(ORDEN);
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
        @DisplayName("findByHospitalizationMedicationId trae todas las tomas de la orden")
        void find_by_hospitalization_medication_id_trae_todas_las_tomas() {
            guardar(PRIMERA_TOMA);
            guardar(PRIMERA_TOMA.plusHours(8));

            assertThat(repository.findByHospitalizationMedicationId(MEDICATION)).hasSize(2);
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
        @DisplayName("disableByHospitalizationMedicationId deshabilita todas las tomas de la orden")
        void disable_by_hospitalization_medication_id_deshabilita_todas() {
            guardar(PRIMERA_TOMA);
            guardar(PRIMERA_TOMA.plusHours(8));

            repository.disableByHospitalizationMedicationId(MEDICATION);
            entityManager.clear();

            assertThat(repository.findByHospitalizationMedicationId(MEDICATION)).isEmpty();
        }

        @Test
        @DisplayName("disablePendingByHospitalizationMedicationId conserva las ya aplicadas")
        void disable_pending_conserva_las_aplicadas() {
            guardar(PRIMERA_TOMA.plusHours(8));
            MedicationSchedule aplicada = repository.save(MedicationSchedule.create(ORDEN,
                    PRIMERA_TOMA, PRIMERA_TOMA, AppliedStatus.APPLIED, false, EMPLEADO));
            entityManager.flush();
            entityManager.clear();

            repository.disablePendingByHospitalizationMedicationId(MEDICATION);
            entityManager.clear();

            assertThat(repository.findByHospitalizationMedicationId(MEDICATION))
                    .extracting(MedicationSchedule::getId).containsExactly(aplicada.getId());
        }

    }

    @Nested
    @DisplayName("Tenancy")
    class Tenancy {

        private MedicationSchedule guardarAjena(LocalDateTime cuando) {
            MedicationSchedule guardada = repository.save(MedicationSchedule.create(ORDEN_AJENA,
                    cuando, cuando, AppliedStatus.PENDING, false, EMPLEADO));
            entityManager.flush();
            entityManager.clear();
            return guardada;
        }

        @Test
        @DisplayName("findByHospitalizationMedicationIdAndCompanyId no ve la orden de otra empresa")
        void find_by_medication_acotado_no_ve_la_orden_ajena() {
            guardarAjena(PRIMERA_TOMA);

            assertThat(repository.findByHospitalizationMedicationIdAndCompanyId(MEDICATION_AJENA,
                    SchemaSeed.COMPANY_ID)).isEmpty();
            assertThat(repository.findByHospitalizationMedicationIdAndCompanyId(MEDICATION_AJENA,
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
        @DisplayName("disableByHospitalizationMedicationId acotado no toca las tomas ajenas")
        void disable_acotado_no_toca_las_tomas_ajenas() {
            guardarAjena(PRIMERA_TOMA);
            guardarAjena(PRIMERA_TOMA.plusHours(8));

            repository.disableByHospitalizationMedicationId(MEDICATION_AJENA,
                    SchemaSeed.COMPANY_ID);
            entityManager.clear();

            assertThat(repository.findByHospitalizationMedicationId(MEDICATION_AJENA)).hasSize(2);
        }

        @Test
        @DisplayName("disablePendingByHospitalizationMedicationId acotado no toca las tomas ajenas")
        void disable_pending_acotado_no_toca_las_tomas_ajenas() {
            guardarAjena(PRIMERA_TOMA);

            repository.disablePendingByHospitalizationMedicationId(MEDICATION_AJENA,
                    SchemaSeed.COMPANY_ID);
            entityManager.clear();

            assertThat(repository.findByHospitalizationMedicationId(MEDICATION_AJENA)).hasSize(1);
        }

        @Test
        @DisplayName("con su propia empresa el UPDATE acotado si deshabilita")
        void con_su_empresa_el_update_acotado_si_deshabilita() {
            guardarAjena(PRIMERA_TOMA);

            repository.disableByHospitalizationMedicationId(MEDICATION_AJENA,
                    SchemaSeed.OTRA_COMPANY_ID);
            entityManager.clear();

            assertThat(repository.findByHospitalizationMedicationId(MEDICATION_AJENA)).isEmpty();
        }
    }
}
