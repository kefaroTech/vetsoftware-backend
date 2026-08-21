package com.vetsoftware.app.consultation.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.consultation.domain.AnimalRef;
import com.vetsoftware.app.consultation.domain.CompanyRef;
import com.vetsoftware.app.consultation.domain.Consultation;
import com.vetsoftware.app.consultation.domain.ConsultationTypeRef;
import com.vetsoftware.app.consultation.domain.PhysicalExam;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import com.vetsoftware.app.testsupport.PersistenceSliceConfig;
import com.vetsoftware.app.testsupport.SchemaSeed;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

/**
 * Rodaja de persistencia de las consultas contra MySQL real (BE-10).
 *
 * <p>
 * Las filas raiz (propietario, especie/raza/color, animal, tipo de consulta) se
 * siembran por SQL nativo: lo que se prueba es la consulta contra la tabla
 * {@code consultations}, no el camino de escritura de esas features hermanas.
 */
@Import(PersistenceSliceConfig.class)
@DisplayName("JpaConsultationRepository — consultas contra MySQL real")
class ConsultationPersistenceIT extends AbstractDataJpaTest {

    private static final Long EMPRESA = SchemaSeed.COMPANY_ID;
    private static final Long OTRA_EMPRESA = SchemaSeed.OTRA_COMPANY_ID;

    /** Padres propios de esta rodaja (ids 970+, fuera del rango de SchemaSeed). */
    private static final Long OWNER = 970L;
    private static final Long SPECIE = 971L;
    private static final Long BREED = 972L;
    private static final Long COLOR = 973L;
    private static final Long ANIMAL = 974L;
    private static final Long CONSULTATION_TYPE = 975L;

    private static final AnimalRef FIRULAIS = new AnimalRef(ANIMAL, "Firulais", "A-001");
    private static final ConsultationTypeRef CONTROL = new ConsultationTypeRef(CONSULTATION_TYPE,
            "Control");
    private static final CompanyRef CLINICA = new CompanyRef(EMPRESA, "Veterinaria de prueba",
            "900123456");
    private static final CompanyRef CLINICA_AJENA = new CompanyRef(OTRA_EMPRESA,
            "Veterinaria ajena", "900654321");

    private static final LocalDate FECHA = LocalDate.of(2026, 3, 1);

    @Autowired
    private JpaConsultationRepository repository;

    @PersistenceContext
    private EntityManager entityManager;

    @BeforeEach
    void sembrarLasFilasRaiz() {
        SchemaSeed.seed(entityManager);
        propietario();
        catalogoDeAnimal();
        animal();
        tipoDeConsulta();
        entityManager.flush();

        // Guardia de la siembra: todo va con INSERT IGNORE y MySQL degrada a warning
        // tanto el NOT NULL sin valor como la FK rota, asi que la fila simplemente no
        // se
        // inserta y nadie se entera. Sin esto el sintoma aparece dos tablas mas abajo,
        // al
        // guardar la consulta, y apunta a la tabla equivocada.
        assertThat(filas("animals", ANIMAL)).as("el animal de las consultas").isOne();
        assertThat(filas("consultation_types", CONSULTATION_TYPE)).as("el tipo de consulta")
                .isOne();
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

    private void tipoDeConsulta() {
        entityManager.createNativeQuery("""
                INSERT IGNORE INTO consultation_types (id, name, description, created_date, enabled)
                VALUES (:id, 'Control', 'Consulta de control', '2026-01-01 08:00:00', true)
                """).setParameter("id", CONSULTATION_TYPE).executeUpdate();
    }

    private Consultation nueva(AnimalRef animal, CompanyRef company) {
        return Consultation.create(FECHA, CONTROL, "Anamnesis del paciente", "Diagnostico",
                "Pronostico reservado", new PhysicalExam(new BigDecimal("38.5"), 90, 22, "Rosadas",
                        "< 2 seg", "Normal", 5, 2, "Alerta", "Sin hallazgos"),
                FECHA.plusDays(15), animal, company);
    }

    private Long guardar(AnimalRef animal, CompanyRef company) {
        Long id = repository.save(nueva(animal, company)).getId();
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
            Long id = guardar(FIRULAIS, CLINICA);

            Consultation releida = repository.findByIdAndCompanyId(id, EMPRESA).orElseThrow();

            assertThat(releida.getAnimal()).isEqualTo(FIRULAIS);
            assertThat(releida.getConsultationType()).isEqualTo(CONTROL);
            assertThat(releida.getCompany()).isEqualTo(CLINICA);
            assertThat(releida.getAnamnesis()).isEqualTo("Anamnesis del paciente");
            assertThat(releida.getPhysicalExam().temperature()).isEqualByComparingTo("38.5");
            assertThat(releida.getPhysicalExam().heartRate()).isEqualTo(90);
        }

        @Test
        @DisplayName("findById a secas no filtra por empresa")
        void find_by_id_a_secas_no_filtra_por_empresa() {
            Long id = guardar(FIRULAIS, CLINICA);

            assertThat(repository.findById(id)).isPresent();
        }

        @Test
        @DisplayName("una consulta de otra empresa no se lee: el filtro va en la consulta")
        void una_consulta_de_otra_empresa_no_se_lee() {
            Long id = guardar(FIRULAIS, CLINICA);

            assertThat(repository.findByIdAndCompanyId(id, OTRA_EMPRESA)).isEmpty();
        }
    }

    @Nested
    @DisplayName("Listados")
    class Listados {

        @Test
        @DisplayName("findAll trae todas las consultas habilitadas")
        void find_all_trae_todas_las_habilitadas() {
            Long uno = guardar(FIRULAIS, CLINICA);
            Long dos = guardar(FIRULAIS, CLINICA);

            assertThat(repository.findAll()).extracting(Consultation::getId).contains(uno, dos);
        }

        @Test
        @DisplayName("findAllByCompanyId pagina y filtra por empresa, la mas reciente primero")
        void find_all_by_company_id_pagina_y_filtra_por_empresa() {
            guardar(FIRULAIS, CLINICA_AJENA);
            Long primera = guardar(FIRULAIS, CLINICA);
            Long segunda = guardar(FIRULAIS, CLINICA);

            PageResult<Consultation> pagina = repository.findAllByCompanyId(EMPRESA, 0, 20);

            assertThat(pagina.content()).extracting(Consultation::getId).containsExactly(segunda,
                    primera);
            assertThat(pagina.totalElements()).isEqualTo(2);
        }
    }
}
