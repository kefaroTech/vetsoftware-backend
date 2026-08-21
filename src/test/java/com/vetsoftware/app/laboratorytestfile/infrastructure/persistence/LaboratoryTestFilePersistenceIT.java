package com.vetsoftware.app.laboratorytestfile.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.laboratorytestfile.domain.EmployeeRef;
import com.vetsoftware.app.laboratorytestfile.domain.LaboratoryTestFile;
import com.vetsoftware.app.laboratorytestfile.domain.LaboratoryTestRef;
import com.vetsoftware.app.laboratorytestfile.domain.LaboratoryTestStoragePathRef;
import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import com.vetsoftware.app.testsupport.PersistenceSliceConfig;
import com.vetsoftware.app.testsupport.SchemaSeed;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

/**
 * Rodaja de persistencia de los archivos de examen de laboratorio contra MySQL
 * real. Cubre en un solo test los tres adaptadores JPA de la feature que
 * comparten la misma cadena de FKs sembrada aqui:
 * {@link JpaLaboratoryTestFileRepository}, {@link JpaLaboratoryTestQueryPort} y
 * {@link JpaEmployeeQueryPort}.
 *
 * <p>
 * <b>Por que un doble no sirve aqui.</b>
 *
 * <ul>
 * <li><b>Los dos {@code @EntityGraph}</b> de
 * {@code LaboratoryTestFileJpaRepository} son lo que evita el N+1 al leer
 * {@code uploadedBy} y {@code laboratoryTest}: solo se ve pasando por
 * Hibernate.</li>
 * <li><b>{@code findStoragePath}</b> navega tres asociaciones
 * ({@code laboratoryTest -> animal -> owner}) en una sola consulta: es SQL
 * real, no logica Java.</li>
 * <li><b>{@code SchemaSeed} no siembra un examen de laboratorio</b> ni la
 * cadena animal/dueno que este necesita, asi que este test completa esa cadena
 * con sus propias filas (ids 970+, fuera del rango de {@link SchemaSeed}).</li>
 * </ul>
 */
@Import(PersistenceSliceConfig.class)
@DisplayName("Adaptadores JPA de laboratorytestfile contra MySQL real")
class LaboratoryTestFilePersistenceIT extends AbstractDataJpaTest {

    private static final Long COMPANY_ID = SchemaSeed.COMPANY_ID;
    private static final Long OTRA_COMPANY_ID = SchemaSeed.OTRA_COMPANY_ID;
    private static final Long EMPLOYEE_ID = SchemaSeed.EMPLOYEE_ID;

    private static final Long SPECIE_ID = 970L;
    private static final Long BREED_ID = 971L;
    private static final Long OWNER_ID = 972L;
    private static final Long COLOR_ID = 973L;
    private static final Long ANIMAL_ID = 974L;
    private static final Long TEST_TYPE_ID = 975L;
    private static final Long LABORATORY_TEST_ID = 976L;
    private static final Long OTRO_LABORATORY_TEST_ID = 977L;

    private static final LocalDateTime CREADO = LocalDateTime.of(2026, 1, 15, 10, 30);

    @Autowired
    private JpaLaboratoryTestFileRepository repository;
    @Autowired
    private JpaLaboratoryTestQueryPort laboratoryTestQueryPort;
    @Autowired
    private JpaEmployeeQueryPort employeeQueryPort;

    @PersistenceContext
    private EntityManager entityManager;

    @BeforeEach
    void sembrarLasFilasRaiz() {
        SchemaSeed.seed(entityManager);
        entityManager.createNativeQuery("""
                INSERT IGNORE INTO species (id, name, created_date, enabled)
                VALUES (:id, 'Perro-LTF', '2026-01-01 00:00:00', true)
                """).setParameter("id", SPECIE_ID).executeUpdate();
        entityManager.createNativeQuery("""
                INSERT IGNORE INTO breeds (id, name, specie_id, created_date, enabled)
                VALUES (:id, 'Labrador-LTF', :specie, '2026-01-01 00:00:00', true)
                """).setParameter("id", BREED_ID).setParameter("specie", SPECIE_ID).executeUpdate();
        entityManager.createNativeQuery("""
                INSERT IGNORE INTO animal_colors (id, name, specie_id, created_date, enabled)
                VALUES (:id, 'Negro-LTF', :specie, '2026-01-01 00:00:00', true)
                """).setParameter("id", COLOR_ID).setParameter("specie", SPECIE_ID).executeUpdate();
        entityManager.createNativeQuery("""
                INSERT IGNORE INTO owners (id, name, document, document_type, person_type,
                                           withholding_agent, tax_regime, fiscal_responsibility,
                                           city_id, company_id, created_date, enabled)
                VALUES (:id, 'Ana Ruiz', 'CC-LTF-1', 'CEDULA_CIUDADANIA', 'NATURAL', false,
                        'NO_RESPONSABLE_IVA', 'NO_APLICA', :ciudad, :empresa,
                        '2026-01-01 00:00:00', true)
                """).setParameter("id", OWNER_ID).setParameter("ciudad", SchemaSeed.CITY_ID)
                .setParameter("empresa", COMPANY_ID).executeUpdate();
        entityManager.createNativeQuery("""
                INSERT IGNORE INTO animals (id, name, code, specie_id, breed_id, owner_id, gender,
                                            weight_type, animal_type, reproductive_state, color_id,
                                            deceased, company_id, created_date, enabled)
                VALUES (:id, 'Firulais', 'A-LTF', :specie, :breed, :owner, 'MALE', 'KILOGRAMS',
                        'NONE', 'STERILIZED', :color, false, :empresa, '2026-01-01 00:00:00', true)
                """).setParameter("id", ANIMAL_ID).setParameter("specie", SPECIE_ID)
                .setParameter("breed", BREED_ID).setParameter("owner", OWNER_ID)
                .setParameter("color", COLOR_ID).setParameter("empresa", COMPANY_ID)
                .executeUpdate();
        entityManager.createNativeQuery("""
                INSERT IGNORE INTO laboratory_test_types (id, name, description, general,
                                                          created_date, enabled)
                VALUES (:id, 'Hemograma-LTF', 'Tipo de prueba', true, '2026-01-01 00:00:00', true)
                """).setParameter("id", TEST_TYPE_ID).executeUpdate();
        laboratoryTest(LABORATORY_TEST_ID, COMPANY_ID);
        laboratoryTest(OTRO_LABORATORY_TEST_ID, COMPANY_ID);
        entityManager.flush();
    }

    private void laboratoryTest(Long id, Long companyId) {
        entityManager.createNativeQuery("""
                INSERT IGNORE INTO laboratory_tests (id, date, test_type_id, quantity, status,
                                                     prioridad, animal_id, company_id, branch_id,
                                                     created_date, enabled)
                VALUES (:id, '2026-01-15', :testType, 1, 'PENDING_COLLECTION', 'NORMAL', :animal,
                        :empresa, :sede, '2026-01-01 00:00:00', true)
                """).setParameter("id", id).setParameter("testType", TEST_TYPE_ID)
                .setParameter("animal", ANIMAL_ID).setParameter("empresa", companyId)
                .setParameter("sede", SchemaSeed.BRANCH_ID).executeUpdate();
    }

    private LaboratoryTestFile archivoNuevo(EmployeeRef uploadedBy,
            LaboratoryTestRef laboratoryTest) {
        return new LaboratoryTestFile(null, "9/3/firulais-100/uuid-informe.pdf",
                "vetsoftware-lab-files", "informe.pdf", "application/pdf", 20480L, "etag-abc",
                uploadedBy, laboratoryTest, CREADO);
    }

    @Nested
    @DisplayName("JpaLaboratoryTestFileRepository — ida y vuelta del agregado")
    class IdaYVuelta {

        @Test
        @DisplayName("guardar y releer conserva cada campo y resuelve empleado y examen desde sus asociaciones")
        void guardar_y_releer_conserva_cada_campo() {
            EmployeeRef veterinario = employeeQueryPort.findById(EMPLOYEE_ID).orElseThrow();
            LaboratoryTestRef examen = laboratoryTestQueryPort
                    .findByIdAndCompanyId(LABORATORY_TEST_ID, COMPANY_ID).orElseThrow();

            LaboratoryTestFile guardado = repository.save(archivoNuevo(veterinario, examen));
            entityManager.flush();
            entityManager.clear();

            LaboratoryTestFile leido = repository.findById(guardado.getId()).orElseThrow();
            assertThat(leido.getOriginalFileName()).isEqualTo("informe.pdf");
            assertThat(leido.getContentType()).isEqualTo("application/pdf");
            assertThat(leido.getSizeBytes()).isEqualTo(20480L);
            assertThat(leido.getBucket()).isEqualTo("vetsoftware-lab-files");
            assertThat(leido.getUploadedBy().id()).isEqualTo(EMPLOYEE_ID);
            assertThat(leido.getLaboratoryTest().id()).isEqualTo(LABORATORY_TEST_ID);
        }
    }

    @Nested
    @DisplayName("JpaLaboratoryTestFileRepository — aislamiento por empresa")
    class AislamientoPorEmpresa {

        @Test
        @DisplayName("findByIdAndCompanyId no devuelve un archivo de otra empresa")
        void find_by_id_and_company_id_no_devuelve_el_de_otra_empresa() {
            EmployeeRef veterinario = employeeQueryPort.findById(EMPLOYEE_ID).orElseThrow();
            LaboratoryTestRef examen = laboratoryTestQueryPort
                    .findByIdAndCompanyId(LABORATORY_TEST_ID, COMPANY_ID).orElseThrow();
            LaboratoryTestFile guardado = repository.save(archivoNuevo(veterinario, examen));
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findByIdAndCompanyId(guardado.getId(), OTRA_COMPANY_ID))
                    .isEmpty();
            assertThat(repository.findByIdAndCompanyId(guardado.getId(), COMPANY_ID)).isPresent();
        }
    }

    @Nested
    @DisplayName("JpaLaboratoryTestFileRepository — listado por examen")
    class ListadoPorExamen {

        @Test
        @DisplayName("devuelve solo los archivos del examen pedido, dentro de su empresa")
        void devuelve_solo_los_archivos_del_examen_pedido() {
            EmployeeRef veterinario = employeeQueryPort.findById(EMPLOYEE_ID).orElseThrow();
            LaboratoryTestRef examen = laboratoryTestQueryPort
                    .findByIdAndCompanyId(LABORATORY_TEST_ID, COMPANY_ID).orElseThrow();
            LaboratoryTestRef otroExamen = laboratoryTestQueryPort
                    .findByIdAndCompanyId(OTRO_LABORATORY_TEST_ID, COMPANY_ID).orElseThrow();
            LaboratoryTestFile delExamen = repository.save(archivoNuevo(veterinario, examen));
            repository.save(archivoNuevo(veterinario, otroExamen));
            entityManager.flush();
            entityManager.clear();

            List<LaboratoryTestFile> encontrados = repository
                    .findAllByLaboratoryTestIdAndCompanyId(LABORATORY_TEST_ID, COMPANY_ID);

            assertThat(encontrados).extracting(LaboratoryTestFile::getId)
                    .containsExactly(delExamen.getId());
        }

        @Test
        @DisplayName("una empresa sin archivos para ese examen recibe una lista vacia")
        void empresa_sin_archivos_recibe_lista_vacia() {
            assertThat(repository.findAllByLaboratoryTestIdAndCompanyId(LABORATORY_TEST_ID,
                    OTRA_COMPANY_ID)).isEmpty();
        }
    }

    @Nested
    @DisplayName("JpaLaboratoryTestFileRepository — borrado")
    class Borrado {

        @Test
        @DisplayName("borrar quita la fila definitivamente: no hay soft delete aqui")
        void borrar_quita_la_fila() {
            EmployeeRef veterinario = employeeQueryPort.findById(EMPLOYEE_ID).orElseThrow();
            LaboratoryTestRef examen = laboratoryTestQueryPort
                    .findByIdAndCompanyId(LABORATORY_TEST_ID, COMPANY_ID).orElseThrow();
            LaboratoryTestFile guardado = repository.save(archivoNuevo(veterinario, examen));
            entityManager.flush();
            entityManager.clear();

            repository.delete(guardado.getId());
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findById(guardado.getId())).isEmpty();
        }
    }

    @Nested
    @DisplayName("JpaLaboratoryTestQueryPort")
    class LaboratoryTestQuery {

        @Test
        @DisplayName("findById mapea el examen a su ref con la fecha real")
        void find_by_id_mapea_el_examen() {
            Optional<LaboratoryTestRef> ref = laboratoryTestQueryPort
                    .findByIdAndCompanyId(LABORATORY_TEST_ID, COMPANY_ID);

            assertThat(ref)
                    .contains(new LaboratoryTestRef(LABORATORY_TEST_ID, LocalDate.of(2026, 1, 15)));
        }

        @Test
        @DisplayName("un examen inexistente devuelve vacio")
        void examen_inexistente_devuelve_vacio() {
            assertThat(laboratoryTestQueryPort.findByIdAndCompanyId(999999L, COMPANY_ID)).isEmpty();
        }

        @Test
        @DisplayName("findStoragePath navega laboratoryTest -> animal -> owner en una sola consulta")
        void find_storage_path_navega_las_tres_asociaciones() {
            Optional<LaboratoryTestStoragePathRef> ruta = laboratoryTestQueryPort
                    .findStoragePath(LABORATORY_TEST_ID, COMPANY_ID);

            assertThat(ruta).contains(
                    new LaboratoryTestStoragePathRef(COMPANY_ID, OWNER_ID, ANIMAL_ID, "Firulais"));
        }

        @Test
        @DisplayName("findStoragePath de un examen inexistente devuelve vacio")
        void find_storage_path_de_examen_inexistente_devuelve_vacio() {
            assertThat(laboratoryTestQueryPort.findStoragePath(999999L, COMPANY_ID)).isEmpty();
        }

        /**
         * El SQL es la barrera, no la anotacion: aqui se comprueba que el
         * {@code WHERE company_id = ?} existe de verdad contra la base. Con el
         * {@code findById} pelado que habia antes, las dos consultas devolvian el
         * examen de la otra empresa y la subida seguia adelante.
         */
        @Test
        @DisplayName("el examen de otra empresa no se resuelve, ni por id ni por ruta de almacenamiento")
        void el_examen_de_otra_empresa_no_se_resuelve() {
            assertThat(laboratoryTestQueryPort.findByIdAndCompanyId(LABORATORY_TEST_ID,
                    OTRA_COMPANY_ID)).isEmpty();
            assertThat(laboratoryTestQueryPort.findStoragePath(LABORATORY_TEST_ID, OTRA_COMPANY_ID))
                    .isEmpty();
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
            assertThat(employeeQueryPort.findById(999999L)).isEmpty();
        }
    }
}
