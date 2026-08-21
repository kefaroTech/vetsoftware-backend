package com.vetsoftware.app.hospitalization.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.hospitalization.domain.AnimalRef;
import com.vetsoftware.app.hospitalization.domain.CompanyRef;
import com.vetsoftware.app.hospitalization.domain.ConsultationRef;
import com.vetsoftware.app.hospitalization.domain.Hospitalization;
import com.vetsoftware.app.hospitalization.domain.HospitalizationType;
import com.vetsoftware.app.hospitalization.domain.ReasonLeaving;
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
 * Rodaja de persistencia de las hospitalizaciones contra MySQL real (BE-10).
 *
 * <p>
 * <b>El semi-cruce que este test cierra.</b> {@code save} decide con un
 * ternario si la fila lleva {@code consultation_id} o lo deja en NULL —esa es
 * la unica rama del adaptador (ramas_missed=2 de 2 en el JaCoCo de hoy)—, y los
 * dos casos de {@link Escritura} ejercitan cada lado. El resto de la clase es
 * SQL que Spring Data parsea al crear el bean, no al compilar
 * ({@code findAllByAnimalIdAndCompanyId} es JPQL con un filtro de texto
 * opcional): si estuviera mal escrita, el contexto no levantaria y todo este
 * archivo fallaria de una vez.
 *
 * <p>
 * Las filas raiz (animal, consulta) se siembran por SQL nativo: lo que se
 * prueba es la consulta contra la tabla, no el camino de escritura de esas
 * features hijas.
 */
@Import(PersistenceSliceConfig.class)
@DisplayName("JpaHospitalizationRepository — hospitalizaciones contra MySQL real")
class HospitalizationPersistenceIT extends AbstractDataJpaTest {

    private static final Long EMPRESA = SchemaSeed.COMPANY_ID;
    private static final Long OTRA_EMPRESA = SchemaSeed.OTRA_COMPANY_ID;

    /** Padres propios de esta rodaja (ids 960+, fuera del rango de SchemaSeed). */
    private static final Long OWNER = 960L;
    private static final Long SPECIE = 961L;
    private static final Long BREED = 962L;
    private static final Long COLOR = 963L;
    private static final Long ANIMAL = 964L;
    private static final Long OTRO_ANIMAL = 965L;
    private static final Long CONSULTATION_TYPE = 966L;
    private static final Long CONSULTATION = 967L;

    private static final AnimalRef FIRULAIS = new AnimalRef(ANIMAL, "Firulais", "A-001");
    private static final AnimalRef MICHI = new AnimalRef(OTRO_ANIMAL, "Michi", "A-002");
    private static final ConsultationRef CONSULTA = new ConsultationRef(CONSULTATION,
            LocalDate.of(2026, 2, 20));
    private static final CompanyRef CLINICA = new CompanyRef(EMPRESA, "Veterinaria de prueba",
            "900123456");
    private static final CompanyRef CLINICA_AJENA = new CompanyRef(OTRA_EMPRESA,
            "Veterinaria ajena", "900654321");

    private static final LocalDate FECHA = LocalDate.of(2026, 3, 1);
    private static final LocalDate INICIO = LocalDate.of(2026, 3, 1);
    private static final LocalDate FIN = LocalDate.of(2026, 3, 5);

    @Autowired
    private JpaHospitalizationRepository repository;

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
        entityManager.flush();

        // Guardia de la siembra: todo va con INSERT IGNORE y MySQL degrada a warning
        // tanto el NOT NULL sin valor como la FK rota, asi que la fila simplemente no
        // se inserta y nadie se entera. Sin esto el sintoma aparece dos tablas mas
        // abajo, al guardar la hospitalizacion, y apunta a la tabla equivocada.
        assertThat(filas("animals", ANIMAL)).as("el animal de las hospitalizaciones").isOne();
        assertThat(filas("animals", OTRO_ANIMAL)).as("el segundo animal").isOne();
        assertThat(filas("consultations", CONSULTATION)).as("la consulta asociada").isOne();
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
                INSERT IGNORE INTO consultation_types (id, name, description, created_date)
                VALUES (:id, 'Control', 'Consulta de control', '2026-01-01 08:00:00')
                """).setParameter("id", CONSULTATION_TYPE).executeUpdate();
        // Sin therapeutic_plan ni diagnosis_plan: la migracion 171 los elimino de la
        // tabla (las acciones rapidas son ahora el origen de verdad del plan).
        entityManager.createNativeQuery("""
                INSERT IGNORE INTO consultations (id, date, consultation_type_id, anamnesis,
                                                  diagnosis, animal_id, company_id, created_date,
                                                  enabled)
                VALUES (:id, '2026-02-20', :tipo, 'Anamnesis', 'Diagnostico', :animal, :empresa,
                        '2026-02-20 09:00:00', true)
                """).setParameter("id", CONSULTATION).setParameter("tipo", CONSULTATION_TYPE)
                .setParameter("animal", ANIMAL).setParameter("empresa", EMPRESA).executeUpdate();
    }

    private Long guardar(AnimalRef animalRef, CompanyRef company, String reason) {
        Hospitalization h = Hospitalization.create(FECHA, INICIO, FIN,
                HospitalizationType.HOSPITALIZATION, ReasonLeaving.MEDICAL_DISCHARGE, reason, null,
                animalRef, null, company);
        Long id = repository.save(h).getId();
        entityManager.flush();
        entityManager.clear();
        return id;
    }

    private Long guardarConObservaciones(String reason, String observations) {
        Hospitalization h = Hospitalization.create(FECHA, INICIO, FIN,
                HospitalizationType.HOSPITALIZATION, ReasonLeaving.MEDICAL_DISCHARGE, reason,
                observations, FIRULAIS, null, CLINICA);
        Long id = repository.save(h).getId();
        entityManager.flush();
        entityManager.clear();
        return id;
    }

    @Nested
    @DisplayName("Escritura y lectura de la hospitalizacion")
    class Escritura {

        @Test
        @DisplayName("guarda con consulta asociada y la devuelve al releer")
        void guarda_con_consulta_asociada_y_la_devuelve_al_releer() {
            Hospitalization nueva = Hospitalization.create(FECHA, INICIO, FIN,
                    HospitalizationType.HOSPITALIZATION, ReasonLeaving.MEDICAL_DISCHARGE,
                    "Gastroenteritis aguda", "Sin complicaciones", FIRULAIS, CONSULTA, CLINICA);

            Hospitalization guardada = repository.save(nueva);
            entityManager.flush();
            entityManager.clear();

            assertThat(guardada.getId()).isNotNull();
            Hospitalization releida = repository.findByIdAndCompanyId(guardada.getId(), EMPRESA)
                    .orElseThrow();
            assertThat(releida.getConsultation()).isEqualTo(CONSULTA);
            assertThat(releida.getAnimal()).isEqualTo(FIRULAIS);
            assertThat(releida.getReason()).isEqualTo("Gastroenteritis aguda");
        }

        @Test
        @DisplayName("guarda sin consulta asociada: la columna queda NULL, no una FK rota")
        void guarda_sin_consulta_asociada() {
            Hospitalization ambulatoria = Hospitalization.create(FECHA, INICIO, null,
                    HospitalizationType.OUTPATIENT, null, "Control post quirurgico", null, FIRULAIS,
                    null, CLINICA);

            Hospitalization guardada = repository.save(ambulatoria);
            entityManager.flush();
            entityManager.clear();

            Hospitalization releida = repository.findById(guardada.getId()).orElseThrow();
            assertThat(releida.getConsultation()).isNull();
            assertThat(releida.getType()).isEqualTo(HospitalizationType.OUTPATIENT);
        }

        @Test
        @DisplayName("una hospitalizacion de otra empresa no se lee: el filtro va en la consulta")
        void una_hospitalizacion_de_otra_empresa_no_se_lee() {
            Long id = guardar(FIRULAIS, CLINICA, "Control");

            assertThat(repository.findByIdAndCompanyId(id, OTRA_EMPRESA)).isEmpty();
        }
    }

    @Nested
    @DisplayName("Listados")
    class Listados {

        @Test
        @DisplayName("findAll trae todas las hospitalizaciones habilitadas")
        void find_all_trae_todas_las_habilitadas() {
            Long uno = guardar(FIRULAIS, CLINICA, "Motivo uno");
            Long dos = guardar(FIRULAIS, CLINICA, "Motivo dos");

            assertThat(repository.findAll()).extracting(Hospitalization::getId).contains(uno, dos);
        }

        @Test
        @DisplayName("findAllByCompanyId solo trae las de la empresa pedida")
        void find_all_by_company_id_filtra_por_empresa() {
            Long propia = guardar(FIRULAIS, CLINICA, "Propia");
            guardar(FIRULAIS, CLINICA_AJENA, "Ajena");

            assertThat(repository.findAllByCompanyId(EMPRESA)).extracting(Hospitalization::getId)
                    .containsExactly(propia);
        }
    }

    @Nested
    @DisplayName("Historial paginado por animal")
    class HistorialPorAnimal {

        @Test
        @DisplayName("pagina el historial del animal, con lo mas reciente primero")
        void pagina_el_historial_mas_reciente_primero() {
            guardar(FIRULAIS, CLINICA, "Primera");
            Long segunda = guardar(FIRULAIS, CLINICA, "Segunda");
            Long tercera = guardar(FIRULAIS, CLINICA, "Tercera");

            PageResult<Hospitalization> pagina = repository.findAllByAnimalIdAndCompanyId(ANIMAL,
                    EMPRESA, null, 0, 2);

            assertThat(pagina.content()).extracting(Hospitalization::getId).containsExactly(tercera,
                    segunda);
            assertThat(pagina.totalElements()).isEqualTo(3);
        }

        @Test
        @DisplayName("filtra por texto en el motivo o en las observaciones")
        void filtra_por_texto_en_motivo_u_observaciones() {
            guardarConObservaciones("Cirugia de urgencia", "Sin novedad");
            Long buscada = guardarConObservaciones("Control", "Requiere dieta especial");

            PageResult<Hospitalization> pagina = repository.findAllByAnimalIdAndCompanyId(ANIMAL,
                    EMPRESA, "dieta", 0, 20);

            assertThat(pagina.content()).extracting(Hospitalization::getId)
                    .containsExactly(buscada);
        }

        @Test
        @DisplayName("no mezcla el historial de otro animal de la misma empresa")
        void no_mezcla_el_historial_de_otro_animal() {
            Long deFirulais = guardar(FIRULAIS, CLINICA, "De Firulais");
            guardar(MICHI, CLINICA, "De Michi");

            PageResult<Hospitalization> pagina = repository.findAllByAnimalIdAndCompanyId(ANIMAL,
                    EMPRESA, null, 0, 20);

            assertThat(pagina.content()).extracting(Hospitalization::getId)
                    .containsExactly(deFirulais);
        }
    }

    @Nested
    @DisplayName("Baja y reactivacion")
    class BajaYReactivacion {

        @Test
        @DisplayName("eliminar aplica el soft delete y reactivate la revive")
        void eliminar_y_reactivar() {
            Long id = guardar(FIRULAIS, CLINICA, "Motivo");

            repository.delete(id);
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findById(id)).isEmpty();

            int filasActualizadas = repository.reactivate(id, EMPRESA);
            entityManager.clear();

            assertThat(filasActualizadas).isOne();
            assertThat(repository.findById(id)).isPresent();
        }

        /**
         * El caso que el SQL tiene que rechazar: en reactivacion no hay lectura previa
         * que valide la propiedad, asi que el {@code AND company_id} del UPDATE es la
         * unica barrera. Cero filas afectadas y la fila sigue deshabilitada.
         */
        @Test
        @DisplayName("reactivate() con el companyId de otra empresa no reactiva nada y la fila sigue deshabilitada")
        void reactivate_con_otra_empresa_no_afecta_filas() {
            Long id = guardar(FIRULAIS, CLINICA, "Motivo");

            repository.delete(id);
            entityManager.flush();
            entityManager.clear();

            int filasActualizadas = repository.reactivate(id, OTRA_EMPRESA);
            entityManager.clear();

            assertThat(filasActualizadas).isZero();
            assertThat(repository.findById(id)).isEmpty();
        }
    }
}
