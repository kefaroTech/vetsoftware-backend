package com.vetsoftware.app.hospitalizationprogressnote.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.hospitalizationprogressnote.domain.EmployeeRef;
import com.vetsoftware.app.hospitalizationprogressnote.domain.HospitalizationProgressNote;
import com.vetsoftware.app.hospitalizationprogressnote.domain.HospitalizationRef;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import com.vetsoftware.app.testsupport.PersistenceSliceConfig;
import com.vetsoftware.app.testsupport.SchemaSeed;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

/**
 * Rodaja de persistencia de las notas de evolucion contra MySQL real (BE-10).
 *
 * <p>
 * Las filas raiz (owner, catalogo de animal, animal, hospitalizacion) se
 * siembran por SQL nativo, igual que en {@code HospitalizationPersistenceIT}:
 * lo que se prueba aqui es la consulta de esta tabla, no el camino de escritura
 * de las features de las que cuelga.
 */
@Import(PersistenceSliceConfig.class)
@DisplayName("JpaHospitalizationProgressNoteRepository — notas de evolucion contra MySQL real")
class HospitalizationProgressNotePersistenceIT extends AbstractDataJpaTest {

    private static final Long EMPRESA = SchemaSeed.COMPANY_ID;
    private static final Long OTRA_EMPRESA = SchemaSeed.OTRA_COMPANY_ID;
    private static final Long EMPLEADO = SchemaSeed.EMPLOYEE_ID;

    /**
     * Padres propios de esta rodaja (ids 980+, fuera del rango de SchemaSeed y de
     * hospitalization).
     */
    private static final Long OWNER = 980L;
    private static final Long SPECIE = 981L;
    private static final Long BREED = 982L;
    private static final Long COLOR = 983L;
    private static final Long ANIMAL = 984L;
    private static final Long HOSPITALIZACION = 985L;
    private static final Long HOSPITALIZACION_AJENA = 986L;
    private static final Long OTRA_HOSPITALIZACION = 987L;

    private static final HospitalizationRef LA_HOSPITALIZACION = new HospitalizationRef(
            HOSPITALIZACION, LocalDate.of(2026, 3, 1));
    private static final HospitalizationRef LA_OTRA_HOSPITALIZACION = new HospitalizationRef(
            OTRA_HOSPITALIZACION, LocalDate.of(2026, 3, 2));
    private static final EmployeeRef EL_VETERINARIO = new EmployeeRef(EMPLEADO, "EMP-001",
            "Ana Ruiz");

    @Autowired
    private JpaHospitalizationProgressNoteRepository repository;

    @PersistenceContext
    private EntityManager entityManager;

    @BeforeEach
    void sembrarLasFilasRaiz() {
        SchemaSeed.seed(entityManager);
        propietario();
        catalogoDeAnimal();
        animal();
        hospitalizacion(HOSPITALIZACION, EMPRESA);
        hospitalizacion(HOSPITALIZACION_AJENA, OTRA_EMPRESA);
        hospitalizacionConFecha(OTRA_HOSPITALIZACION, EMPRESA, LocalDate.of(2026, 3, 2));
        entityManager.flush();

        // Guardia de la siembra: INSERT IGNORE degrada silenciosamente FK rotas a un
        // warning,
        // asi que sin esto el sintoma aparece dos tablas mas abajo, al guardar la nota.
        assertThat(filas("animals", ANIMAL)).as("el animal de la hospitalizacion").isOne();
        assertThat(filas("hospitalizations", HOSPITALIZACION)).as("la hospitalizacion").isOne();
    }

    private long filas(String tabla, Long id) {
        Number total = (Number) entityManager
                .createNativeQuery("SELECT COUNT(*) FROM " + tabla + " WHERE id = :id")
                .setParameter("id", id).getSingleResult();
        return total.longValue();
    }

    private void propietario() {
        entityManager.createNativeQuery("""
                INSERT IGNORE INTO owners (id, name, document, document_type, person_type,
                                           withholding_agent, tax_regime, fiscal_responsibility,
                                           city_id, company_id, created_date, enabled)
                VALUES (:id, 'Marta Diaz', 'CC-9001', 'CEDULA_CIUDADANIA', 'NATURAL', false,
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
                VALUES (:id, 'Firulais', 'A-900', :specie, :breed, :owner, 'MALE', 'KILOGRAMS',
                        'NONE', 'UNKNOWN', :color, false, :empresa, '2026-01-01 08:00:00', true)
                """).setParameter("id", ANIMAL).setParameter("specie", SPECIE)
                .setParameter("breed", BREED).setParameter("owner", OWNER)
                .setParameter("color", COLOR).setParameter("empresa", EMPRESA).executeUpdate();
    }

    private void hospitalizacion(Long id, Long companyId) {
        hospitalizacionConFecha(id, companyId, LocalDate.of(2026, 3, 1));
    }

    private void hospitalizacionConFecha(Long id, Long companyId, LocalDate fecha) {
        entityManager
                .createNativeQuery(
                        """
                                INSERT IGNORE INTO hospitalizations (id, date, start_date, end_date, type,
                                                                     reason_leaving, reason, observations, animal_id,
                                                                     company_id, created_date)
                                VALUES (:id, :fecha, :fecha, null, 'HOSPITALIZATION', 'MEDICAL_DISCHARGE',
                                        'Gastroenteritis aguda', 'Sin complicaciones', :animal, :empresa,
                                        '2026-03-01 09:00:00')
                                """)
                .setParameter("id", id).setParameter("fecha", fecha).setParameter("animal", ANIMAL)
                .setParameter("empresa", companyId).executeUpdate();
    }

    private Long guardar(HospitalizationRef hospitalizacion, String descripcion) {
        HospitalizationProgressNote nota = HospitalizationProgressNote.create(descripcion,
                hospitalizacion, EL_VETERINARIO);
        Long id = repository.save(nota).getId();
        entityManager.flush();
        entityManager.clear();
        return id;
    }

    @Nested
    @DisplayName("Escritura y lectura de la nota")
    class Escritura {

        @Test
        @DisplayName("guarda la nota y la devuelve al releer con sus dos referencias hidratadas")
        void guarda_y_la_devuelve_al_releer() {
            Long id = guardar(LA_HOSPITALIZACION,
                    "Paciente estable, buena respuesta al tratamiento");

            HospitalizationProgressNote releida = repository.findById(id).orElseThrow();

            assertThat(releida.getDescription())
                    .isEqualTo("Paciente estable, buena respuesta al tratamiento");
            assertThat(releida.getHospitalization()).isEqualTo(LA_HOSPITALIZACION);
            assertThat(releida.getCreatedBy()).isEqualTo(EL_VETERINARIO);
            assertThat(releida.isEnabled()).isTrue();
        }
    }

    @Nested
    @DisplayName("Tenancy")
    class Tenancy {

        @Test
        @DisplayName("findByIdAndCompanyId trae la nota si la hospitalizacion es de esa empresa")
        void find_by_id_and_company_id_trae_la_nota_de_su_empresa() {
            Long id = guardar(LA_HOSPITALIZACION, "Control post quirurgico");

            assertThat(repository.findByIdAndCompanyId(id, EMPRESA)).isPresent();
        }

        @Test
        @DisplayName("una nota de otra empresa no se lee: el filtro va en la consulta")
        void una_nota_de_otra_empresa_no_se_lee() {
            Long id = guardar(
                    new HospitalizationRef(HOSPITALIZACION_AJENA, LocalDate.of(2026, 3, 1)),
                    "Nota de la clinica ajena");

            assertThat(repository.findByIdAndCompanyId(id, EMPRESA)).isEmpty();
        }
    }

    @Nested
    @DisplayName("Listado por hospitalizacion")
    class ListadoPorHospitalizacion {

        @Test
        @DisplayName("pagina las notas de la hospitalizacion, con lo mas reciente primero")
        void pagina_las_notas_mas_reciente_primero() {
            guardar(LA_HOSPITALIZACION, "Primera evolucion");
            Long segunda = guardar(LA_HOSPITALIZACION, "Segunda evolucion");
            Long tercera = guardar(LA_HOSPITALIZACION, "Tercera evolucion");

            PageResult<HospitalizationProgressNote> pagina = repository
                    .findAllByHospitalizationIdAndCompanyId(HOSPITALIZACION, EMPRESA, 0, 2);

            assertThat(pagina.content()).extracting(HospitalizationProgressNote::getId)
                    .containsExactly(tercera, segunda);
            assertThat(pagina.totalElements()).isEqualTo(3);
        }

        @Test
        @DisplayName("no mezcla las notas de otra hospitalizacion de la misma empresa")
        void no_mezcla_las_notas_de_otra_hospitalizacion() {
            Long deLaPrimera = guardar(LA_HOSPITALIZACION, "De la primera hospitalizacion");
            guardar(LA_OTRA_HOSPITALIZACION, "De la otra hospitalizacion");

            PageResult<HospitalizationProgressNote> pagina = repository
                    .findAllByHospitalizationIdAndCompanyId(HOSPITALIZACION, EMPRESA, 0, 20);

            assertThat(pagina.content()).extracting(HospitalizationProgressNote::getId)
                    .containsExactly(deLaPrimera);
        }
    }

    @Nested
    @DisplayName("Baja")
    class Baja {

        @Test
        @DisplayName("eliminar aplica el soft delete y la nota deja de verse")
        void eliminar_aplica_soft_delete() {
            Long id = guardar(LA_HOSPITALIZACION, "Nota a eliminar");

            repository.delete(id);
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findById(id)).isEmpty();
        }
    }
}
