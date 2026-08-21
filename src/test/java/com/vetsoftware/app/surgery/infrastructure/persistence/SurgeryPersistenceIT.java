package com.vetsoftware.app.surgery.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.surgery.domain.AnimalRef;
import com.vetsoftware.app.surgery.domain.CompanyRef;
import com.vetsoftware.app.surgery.domain.ConsultationRef;
import com.vetsoftware.app.surgery.domain.Surgery;
import com.vetsoftware.app.surgery.domain.SurgeryTypeRef;
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
 * Rodaja de persistencia de las cirugias contra MySQL real (BE-10).
 *
 * <p>
 * Las filas raiz (propietario, especie/raza/color, animal, tipo de cirugia,
 * tipo/consulta) se siembran por SQL nativo: lo que se prueba es la consulta
 * contra la tabla {@code surgeries}, no el camino de escritura de esas features
 * hermanas.
 */
@Import(PersistenceSliceConfig.class)
@DisplayName("JpaSurgeryRepository — consultas contra MySQL real")
class SurgeryPersistenceIT extends AbstractDataJpaTest {

    private static final Long EMPRESA = SchemaSeed.COMPANY_ID;
    private static final Long OTRA_EMPRESA = SchemaSeed.OTRA_COMPANY_ID;

    /** Padres propios de esta rodaja (ids 980+, fuera del rango de SchemaSeed). */
    private static final Long OWNER = 980L;
    private static final Long SPECIE = 981L;
    private static final Long BREED = 982L;
    private static final Long COLOR = 983L;
    private static final Long ANIMAL = 984L;
    private static final Long SURGERY_TYPE = 985L;
    private static final Long CONSULTATION_TYPE = 986L;
    private static final Long CONSULTATION = 987L;
    private static final Long OTRO_ANIMAL = 988L;

    private static final AnimalRef FIRULAIS = new AnimalRef(ANIMAL, "Firulais", "A-001");
    private static final AnimalRef OTRO_ANIMAL_REF = new AnimalRef(OTRO_ANIMAL, "Michi", "A-002");
    private static final SurgeryTypeRef OVARIOHISTERECTOMIA = new SurgeryTypeRef(SURGERY_TYPE,
            "Ovariohisterectomia");
    private static final ConsultationRef CONSULTA_PREVIA = new ConsultationRef(CONSULTATION,
            LocalDate.of(2026, 3, 9));
    private static final CompanyRef CLINICA = new CompanyRef(EMPRESA, "Veterinaria de prueba",
            "900123456");
    private static final CompanyRef CLINICA_AJENA = new CompanyRef(OTRA_EMPRESA,
            "Veterinaria ajena", "900654321");

    private static final LocalDate FECHA = LocalDate.of(2026, 3, 10);

    @Autowired
    private JpaSurgeryRepository repository;

    @PersistenceContext
    private EntityManager entityManager;

    @BeforeEach
    void sembrarLasFilasRaiz() {
        SchemaSeed.seed(entityManager);
        propietario();
        catalogoDeAnimal();
        animal(ANIMAL, "Firulais", "A-001");
        animal(OTRO_ANIMAL, "Michi", "A-002");
        tipoDeCirugia();
        tipoDeConsulta();
        consulta();
        entityManager.flush();

        // Guardia de la siembra: todo va con INSERT IGNORE y MySQL degrada a warning
        // tanto el
        // NOT NULL sin valor como la FK rota, asi que la fila simplemente no se inserta
        // y nadie
        // se entera. Sin esto el sintoma aparece dos tablas mas abajo, al guardar la
        // cirugia.
        assertThat(filas("animals", ANIMAL)).as("el animal de las cirugias").isOne();
        assertThat(filas("surgery_types", SURGERY_TYPE)).as("el tipo de cirugia").isOne();
        assertThat(filas("consultations", CONSULTATION)).as("la consulta previa").isOne();
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
                VALUES (:id, :nombre, :codigo, :specie, :breed, :owner, 'MALE', 'KILOGRAMS', 'NONE',
                        'UNKNOWN', :color, false, :empresa, '2026-01-01 08:00:00', true)
                """).setParameter("id", id).setParameter("nombre", nombre)
                .setParameter("codigo", codigo).setParameter("specie", SPECIE)
                .setParameter("breed", BREED).setParameter("owner", OWNER)
                .setParameter("color", COLOR).setParameter("empresa", EMPRESA).executeUpdate();
    }

    private void tipoDeCirugia() {
        entityManager.createNativeQuery("""
                INSERT IGNORE INTO surgery_types (id, name, description, general, created_date,
                                                  enabled)
                VALUES (:id, 'Ovariohisterectomia', 'Cirugia de esterilizacion', true,
                        '2026-01-01 08:00:00', true)
                """).setParameter("id", SURGERY_TYPE).executeUpdate();
    }

    private void tipoDeConsulta() {
        entityManager.createNativeQuery("""
                INSERT IGNORE INTO consultation_types (id, name, description, created_date, enabled)
                VALUES (:id, 'Control prequirurgico', 'Valoracion previa a cirugia',
                        '2026-01-01 08:00:00', true)
                """).setParameter("id", CONSULTATION_TYPE).executeUpdate();
    }

    private void consulta() {
        entityManager
                .createNativeQuery(
                        """
                                INSERT IGNORE INTO consultations (id, date, consultation_type_id, anamnesis,
                                                                  animal_id, company_id, created_date, enabled)
                                VALUES (:id, '2026-03-09', :tipo, 'Valoracion previa a la cirugia', :animal, :empresa,
                                        '2026-03-09 08:00:00', true)
                                """)
                .setParameter("id", CONSULTATION).setParameter("tipo", CONSULTATION_TYPE)
                .setParameter("animal", ANIMAL).setParameter("empresa", EMPRESA).executeUpdate();
    }

    private Surgery nueva(AnimalRef animal, ConsultationRef consultation, CompanyRef company) {
        return Surgery.create(FECHA, OVARIOHISTERECTOMIA, "Ovariohisterectomia electiva",
                "Ketamina 10mg", "Recuperacion normal", null, animal, consultation, company);
    }

    private Long guardar(AnimalRef animal, ConsultationRef consultation, CompanyRef company) {
        Long id = repository.save(nueva(animal, consultation, company)).getId();
        entityManager.flush();
        entityManager.clear();
        return id;
    }

    @Nested
    @DisplayName("Escritura y lectura")
    class Escritura {

        @Test
        @DisplayName("guarda y la devuelve al releer con las referencias resueltas")
        void guarda_y_la_devuelve_al_releer() {
            Long id = guardar(FIRULAIS, CONSULTA_PREVIA, CLINICA);

            Surgery releida = repository.findByIdAndCompanyId(id, EMPRESA).orElseThrow();

            assertThat(releida.getAnimal()).isEqualTo(FIRULAIS);
            assertThat(releida.getSurgeryType()).isEqualTo(OVARIOHISTERECTOMIA);
            assertThat(releida.getConsultation()).isEqualTo(CONSULTA_PREVIA);
            assertThat(releida.getCompany()).isEqualTo(CLINICA);
            assertThat(releida.getDescription()).isEqualTo("Ovariohisterectomia electiva");
            assertThat(releida.getStatus().name()).isEqualTo("PROGRAMADA");
        }

        @Test
        @DisplayName("guarda sin consulta asociada: es opcional")
        void guarda_sin_consulta_asociada() {
            Long id = guardar(FIRULAIS, null, CLINICA);

            Surgery releida = repository.findByIdAndCompanyId(id, EMPRESA).orElseThrow();

            assertThat(releida.getConsultation()).isNull();
        }

        @Test
        @DisplayName("findById a secas no filtra por empresa")
        void find_by_id_a_secas_no_filtra_por_empresa() {
            Long id = guardar(FIRULAIS, CONSULTA_PREVIA, CLINICA);

            assertThat(repository.findById(id)).isPresent();
        }

        @Test
        @DisplayName("una cirugia de otra empresa no se lee con findByIdAndCompanyId")
        void una_cirugia_de_otra_empresa_no_se_lee() {
            Long id = guardar(FIRULAIS, CONSULTA_PREVIA, CLINICA);

            assertThat(repository.findByIdAndCompanyId(id, OTRA_EMPRESA)).isEmpty();
        }
    }

    @Nested
    @DisplayName("Listados")
    class Listados {

        @Test
        @DisplayName("findAll trae todas las cirugias habilitadas")
        void find_all_trae_todas_las_habilitadas() {
            Long uno = guardar(FIRULAIS, CONSULTA_PREVIA, CLINICA);
            Long dos = guardar(FIRULAIS, null, CLINICA);

            assertThat(repository.findAll()).extracting(Surgery::getId).contains(uno, dos);
        }

        @Test
        @DisplayName("findAllByAnimalIdAndCompanyId pagina y filtra por animal y empresa, la mas"
                + " reciente primero")
        void find_all_by_animal_id_and_company_id_pagina_y_filtra() {
            guardar(FIRULAIS, null, CLINICA_AJENA);
            guardar(OTRO_ANIMAL_REF, null, CLINICA);
            Long primera = guardar(FIRULAIS, null, CLINICA);
            Long segunda = guardar(FIRULAIS, null, CLINICA);

            PageResult<Surgery> pagina = repository.findAllByAnimalIdAndCompanyId(ANIMAL, EMPRESA,
                    null, 0, 20);

            assertThat(pagina.content()).extracting(Surgery::getId).containsExactly(segunda,
                    primera);
            assertThat(pagina.totalElements()).isEqualTo(2);
        }

        @Test
        @DisplayName("findAllByAnimalIdAndCompanyId filtra por texto en la descripcion")
        void find_all_by_animal_id_and_company_id_filtra_por_texto() {
            guardar(FIRULAIS, null, CLINICA);

            PageResult<Surgery> conCoincidencia = repository.findAllByAnimalIdAndCompanyId(ANIMAL,
                    EMPRESA, "ovariohisterectomia", 0, 20);
            PageResult<Surgery> sinCoincidencia = repository.findAllByAnimalIdAndCompanyId(ANIMAL,
                    EMPRESA, "castracion", 0, 20);

            assertThat(conCoincidencia.totalElements()).isEqualTo(1);
            assertThat(sinCoincidencia.totalElements()).isZero();
        }
    }

    @Nested
    @DisplayName("Baja y reactivacion")
    class BajaYReactivacion {

        @Test
        @DisplayName("delete aplica el soft delete")
        void delete_aplica_el_soft_delete() {
            Long id = guardar(FIRULAIS, null, CLINICA);

            repository.delete(id);
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findById(id)).isEmpty();
        }

        @Test
        @DisplayName("reactivate revive la fila")
        void reactivate_revive_la_fila() {
            Long id = guardar(FIRULAIS, null, CLINICA);
            repository.delete(id);
            entityManager.flush();
            entityManager.clear();

            int filasActualizadas = repository.reactivate(id, EMPRESA);
            entityManager.clear();

            assertThat(filasActualizadas).isOne();
            assertThat(repository.findByIdAndCompanyId(id, EMPRESA)).isPresent();
        }

        @Test
        @DisplayName("reactivate con el companyId de OTRA empresa no afecta ninguna fila y la deja"
                + " borrada")
        void reactivate_con_empresa_ajena_no_afecta_filas() {
            Long id = guardar(FIRULAIS, null, CLINICA_AJENA);
            repository.delete(id);
            entityManager.flush();
            entityManager.clear();

            int filasActualizadas = repository.reactivate(id, EMPRESA);
            entityManager.clear();

            assertThat(filasActualizadas).isZero();
            assertThat(repository.findById(id)).isEmpty();
        }

        @Test
        @DisplayName("reactivate sobre un id inexistente no afecta ninguna fila")
        void reactivate_sobre_id_inexistente_no_afecta_filas() {
            assertThat(repository.reactivate(999_999L, EMPRESA)).isZero();
        }
    }
}
