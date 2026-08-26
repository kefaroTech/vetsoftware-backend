package com.vetsoftware.app.vaccination.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import com.vetsoftware.app.testsupport.PersistenceSliceConfig;
import com.vetsoftware.app.testsupport.SchemaSeed;
import com.vetsoftware.app.vaccination.domain.AnimalRef;
import com.vetsoftware.app.vaccination.domain.CompanyRef;
import com.vetsoftware.app.vaccination.domain.ConsultationRef;
import com.vetsoftware.app.vaccination.domain.Vaccination;
import com.vetsoftware.app.vaccination.domain.VaccinationTypeRef;
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
 * Rodaja de persistencia de las vacunas contra MySQL real (BE-10).
 *
 * <p>
 * <b>El semi-cruce que este test cierra.</b> {@code save} decide con un
 * ternario si la fila lleva {@code consultation_id} o lo deja en NULL — la
 * unica rama del adaptador (ramas_missed=2 de 2 en el JaCoCo de hoy) — y los
 * dos casos de {@link Escritura} ejercitan cada lado. El resto de la clase es
 * SQL que Spring Data parsea al crear el bean: si estuviera mal escrito, el
 * contexto no levantaria y todo este archivo fallaria de una vez.
 *
 * <p>
 * Las filas raiz (animal, consulta, tipo de vacuna) se siembran por SQL nativo:
 * lo que se prueba es la consulta contra la tabla de vacunas, no el camino de
 * escritura de esas features hermanas.
 */
@Import(PersistenceSliceConfig.class)
@DisplayName("JpaVaccinationRepository — vacunas contra MySQL real")
class VaccinationPersistenceIT extends AbstractDataJpaTest {

    private static final Long EMPRESA = SchemaSeed.COMPANY_ID;
    private static final Long OTRA_EMPRESA = SchemaSeed.OTRA_COMPANY_ID;

    /** Padres propios de esta rodaja (ids 980+, fuera del rango de SchemaSeed). */
    private static final Long OWNER = 980L;
    private static final Long SPECIE = 981L;
    private static final Long BREED = 982L;
    private static final Long COLOR = 983L;
    private static final Long ANIMAL = 984L;
    private static final Long OTRO_ANIMAL = 985L;
    private static final Long CONSULTATION_TYPE = 986L;
    private static final Long CONSULTATION = 987L;
    private static final Long VACCINATION_TYPE = 988L;

    private static final AnimalRef FIRULAIS = new AnimalRef(ANIMAL, "Firulais", "A-001");
    private static final AnimalRef MICHI = new AnimalRef(OTRO_ANIMAL, "Michi", "A-002");
    private static final ConsultationRef CONSULTA = new ConsultationRef(CONSULTATION,
            LocalDate.of(2026, 2, 20));
    /**
     * Tipo PROPIO de la empresa: {@code general = false} exige {@code company_id}
     * con valor ({@code ck_vaccination_types_owner_xor}, changeset 286). El sufijo
     * de rodaja lo mantiene fuera del alcance de la semilla del changeset 294, que
     * se compara sin acentos ni caja.
     */
    private static final VaccinationTypeRef RABIA = new VaccinationTypeRef(VACCINATION_TYPE,
            "Rabia-VA");
    private static final CompanyRef CLINICA = new CompanyRef(EMPRESA, "Veterinaria de prueba",
            "900123456");
    private static final CompanyRef CLINICA_AJENA = new CompanyRef(OTRA_EMPRESA,
            "Veterinaria ajena", "900654321");

    private static final LocalDate FECHA = LocalDate.of(2026, 3, 1);
    private static final LocalDate PROXIMA = LocalDate.of(2027, 3, 1);

    @Autowired
    private JpaVaccinationRepository repository;

    @PersistenceContext
    private EntityManager entityManager;

    @BeforeEach
    void sembrarLasFilasRaiz() {
        SchemaSeed.seed(entityManager);
        propietario();
        catalogoDeAnimal();
        animal(ANIMAL, "Firulais", "A-001");
        animal(OTRO_ANIMAL, "Michi", "A-002");
        consultaAsociada();
        tipoDeVacuna(VACCINATION_TYPE, "Rabia-VA");
        entityManager.flush();

        // Guardia de la siembra: INSERT IGNORE degrada la FK rota a warning, asi que
        // sin esto el sintoma aparece dos tablas mas abajo, al guardar la vacuna.
        assertThat(filas("animals", ANIMAL)).as("el animal de las vacunas").isOne();
        assertThat(filas("consultations", CONSULTATION)).as("la consulta asociada").isOne();
        assertThat(filas("vaccination_types", VACCINATION_TYPE)).as("el tipo de vacuna").isOne();
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
                VALUES (:id, 'Marta Diaz', 'CC-1001', 'CEDULA_CIUDADANIA', 'NATURAL', false,
                        'NO_RESPONSABLE_IVA', 'NO_APLICA', :ciudad, :empresa,
                        '2026-01-01 08:00:00', true)
                """).setParameter("id", OWNER).setParameter("ciudad", SchemaSeed.CITY_ID)
                .setParameter("empresa", EMPRESA).executeUpdate();
    }

    private void catalogoDeAnimal() {
        entityManager.createNativeQuery("""
                INSERT IGNORE INTO species (id, name, general, created_date, enabled, version)
                VALUES (:id, 'Canino-VA', true, '2026-01-01 08:00:00', true, 0)
                """).setParameter("id", SPECIE).executeUpdate();
        entityManager.createNativeQuery("""
                INSERT IGNORE INTO breeds (id, name, specie_id, general, created_date, enabled,
                                           version)
                VALUES (:id, 'Criollo-VA', :specie, true, '2026-01-01 08:00:00', true, 0)
                """).setParameter("id", BREED).setParameter("specie", SPECIE).executeUpdate();
        entityManager.createNativeQuery("""
                INSERT IGNORE INTO animal_colors (id, name, specie_id, general, created_date,
                                                  enabled, version)
                VALUES (:id, 'Negro-VA', :specie, true, '2026-01-01 08:00:00', true, 0)
                """).setParameter("id", COLOR).setParameter("specie", SPECIE).executeUpdate();
    }

    private void animal(Long id, String nombre, String codigo) {
        entityManager.createNativeQuery("""
                INSERT IGNORE INTO animals (id, name, code, specie_id, breed_id, owner_id, gender,
                                            weight_type, animal_type, reproductive_state, color_id,
                                            deceased, company_id, created_date, enabled)
                VALUES (:id, :nombre, :codigo, :specie, :breed, :owner, 'MALE', 'KILOGRAMS',
                        'NONE', 'UNKNOWN', :color, false, :empresa, '2026-01-01 08:00:00', true)
                """).setParameter("id", id).setParameter("nombre", nombre)
                .setParameter("codigo", codigo).setParameter("specie", SPECIE)
                .setParameter("breed", BREED).setParameter("owner", OWNER)
                .setParameter("color", COLOR).setParameter("empresa", EMPRESA).executeUpdate();
    }

    private void consultaAsociada() {
        entityManager.createNativeQuery("""
                INSERT IGNORE INTO consultation_types (id, name, description, general,
                                                       created_date, enabled, version)
                VALUES (:id, 'Control-VA', 'Consulta de control', true, '2026-01-01 08:00:00',
                        true, 0)
                """).setParameter("id", CONSULTATION_TYPE).executeUpdate();
        entityManager.createNativeQuery("""
                INSERT IGNORE INTO consultations (id, date, consultation_type_id, anamnesis,
                                                  diagnosis, animal_id, company_id, created_date,
                                                  enabled)
                VALUES (:id, '2026-02-20', :tipo, 'Anamnesis', 'Diagnostico', :animal, :empresa,
                        '2026-02-20 09:00:00', true)
                """).setParameter("id", CONSULTATION).setParameter("tipo", CONSULTATION_TYPE)
                .setParameter("animal", ANIMAL).setParameter("empresa", EMPRESA).executeUpdate();
    }

    /**
     * Fila PROPIA de la empresa: {@code general = false} obliga a llevar
     * {@code company_id}. Sin el, {@code ck_vaccination_types_owner_xor} rechaza el
     * INSERT, el {@code IGNORE} degrada el rechazo a warning y la fila no entra: el
     * fallo aparece mucho despues, contando cero.
     */
    private void tipoDeVacuna(Long id, String nombre) {
        entityManager.createNativeQuery("""
                INSERT IGNORE INTO vaccination_types (id, name, description, general, company_id,
                                                      created_date, enabled, version)
                VALUES (:id, :nombre, 'Vacuna de prueba', false, :empresa,
                        '2026-01-01 08:00:00', true, 0)
                """).setParameter("id", id).setParameter("nombre", nombre)
                .setParameter("empresa", EMPRESA).executeUpdate();
    }

    private Vaccination crear(AnimalRef animal, ConsultationRef consultation, CompanyRef company,
            String lot) {
        return Vaccination.create(FECHA, RABIA, lot, "Sin reaccion", "Subcutanea", "Cuello",
                PROXIMA, animal, consultation, company);
    }

    private Long guardar(AnimalRef animal, ConsultationRef consultation, CompanyRef company,
            String lot) {
        Long id = repository.save(crear(animal, consultation, company, lot)).getId();
        entityManager.flush();
        entityManager.clear();
        return id;
    }

    private Long guardarConNotas(String lot, String notas) {
        Vaccination v = Vaccination.create(FECHA, RABIA, lot, notas, "Subcutanea", "Cuello",
                PROXIMA, FIRULAIS, null, CLINICA);
        Long id = repository.save(v).getId();
        entityManager.flush();
        entityManager.clear();
        return id;
    }

    @Nested
    @DisplayName("Escritura y lectura de la vacuna")
    class Escritura {

        @Test
        @DisplayName("guarda con consulta asociada y la devuelve al releer")
        void guarda_con_consulta_asociada_y_la_devuelve_al_releer() {
            Vaccination nueva = crear(FIRULAIS, CONSULTA, CLINICA, "L-2026-A");

            Vaccination guardada = repository.save(nueva);
            entityManager.flush();
            entityManager.clear();

            assertThat(guardada.getId()).isNotNull();
            Vaccination releida = repository.findByIdAndCompanyId(guardada.getId(), EMPRESA)
                    .orElseThrow();
            assertThat(releida.getConsultation()).isEqualTo(CONSULTA);
            assertThat(releida.getAnimal()).isEqualTo(FIRULAIS);
            assertThat(releida.getVaccinationType()).isEqualTo(RABIA);
            assertThat(releida.getLot()).isEqualTo("L-2026-A");
        }

        @Test
        @DisplayName("guarda sin consulta asociada: la columna queda NULL, no una FK rota")
        void guarda_sin_consulta_asociada() {
            Vaccination sinConsulta = crear(FIRULAIS, null, CLINICA, "L-2026-B");

            Vaccination guardada = repository.save(sinConsulta);
            entityManager.flush();
            entityManager.clear();

            Vaccination releida = repository.findById(guardada.getId()).orElseThrow();
            assertThat(releida.getConsultation()).isNull();
        }

        @Test
        @DisplayName("una vacuna de otra empresa no se lee: el filtro va en la consulta")
        void una_vacuna_de_otra_empresa_no_se_lee() {
            Long id = guardar(FIRULAIS, null, CLINICA, "L-2026-C");

            assertThat(repository.findByIdAndCompanyId(id, OTRA_EMPRESA)).isEmpty();
        }
    }

    @Nested
    @DisplayName("Listados")
    class Listados {

        @Test
        @DisplayName("findAll trae todas las vacunas habilitadas")
        void find_all_trae_todas_las_habilitadas() {
            Long uno = guardar(FIRULAIS, null, CLINICA, "L-2026-D");
            Long dos = guardar(FIRULAIS, null, CLINICA, "L-2026-E");

            assertThat(repository.findAll()).extracting(Vaccination::getId).contains(uno, dos);
        }
    }

    @Nested
    @DisplayName("Historial paginado por animal")
    class HistorialPorAnimal {

        @Test
        @DisplayName("pagina el historial del animal, con lo mas reciente primero")
        void pagina_el_historial_mas_reciente_primero() {
            guardar(FIRULAIS, null, CLINICA, "L-1");
            Long segunda = guardar(FIRULAIS, null, CLINICA, "L-2");
            Long tercera = guardar(FIRULAIS, null, CLINICA, "L-3");

            PageResult<Vaccination> pagina = repository.findAllByAnimalIdAndCompanyId(ANIMAL,
                    EMPRESA, null, 0, 2);

            assertThat(pagina.content()).extracting(Vaccination::getId).containsExactly(tercera,
                    segunda);
            assertThat(pagina.totalElements()).isEqualTo(3);
        }

        @Test
        @DisplayName("filtra por texto en las notas")
        void filtra_por_texto_en_las_notas() {
            guardarConNotas("L-1", "Sin novedad");
            Long buscada = guardarConNotas("L-2", "Requiere seguimiento en dos semanas");

            PageResult<Vaccination> pagina = repository.findAllByAnimalIdAndCompanyId(ANIMAL,
                    EMPRESA, "seguimiento", 0, 20);

            assertThat(pagina.content()).extracting(Vaccination::getId).containsExactly(buscada);
        }

        @Test
        @DisplayName("no mezcla el historial de otro animal de la misma empresa")
        void no_mezcla_el_historial_de_otro_animal() {
            Long deFirulais = guardar(FIRULAIS, null, CLINICA, "L-1");
            guardar(MICHI, null, CLINICA, "L-2");

            PageResult<Vaccination> pagina = repository.findAllByAnimalIdAndCompanyId(ANIMAL,
                    EMPRESA, null, 0, 20);

            assertThat(pagina.content()).extracting(Vaccination::getId).containsExactly(deFirulais);
        }

        @Test
        @DisplayName("no mezcla el historial de otra empresa aunque sea el mismo animal")
        void no_mezcla_el_historial_de_otra_empresa() {
            Long propia = guardar(FIRULAIS, null, CLINICA, "L-1");
            guardar(FIRULAIS, null, CLINICA_AJENA, "L-2");

            PageResult<Vaccination> pagina = repository.findAllByAnimalIdAndCompanyId(ANIMAL,
                    EMPRESA, null, 0, 20);

            assertThat(pagina.content()).extracting(Vaccination::getId).containsExactly(propia);
        }
    }
}
