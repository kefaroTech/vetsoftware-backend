package com.vetsoftware.app.clinicalhistory.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.clinicalhistory.application.dto.ClinicalEventTypeCountDto;
import com.vetsoftware.app.clinicalhistory.application.query.GetClinicalHistoryQuery;
import com.vetsoftware.app.clinicalhistory.application.query.ListCompanyClinicalEventsQuery;
import com.vetsoftware.app.clinicalhistory.domain.ClinicalEvent;
import com.vetsoftware.app.clinicalhistory.domain.ClinicalEventType;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import com.vetsoftware.app.testsupport.SchemaSeed;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

/**
 * Rodaja de persistencia del adaptador {@code JpaClinicalEventRepository}
 * contra MySQL real (BE-10, {@code ADAPTADOR_JPA_CON_RODAJA}).
 *
 * <p>
 * <b>Por qué un doble no sirve.</b> {@code ClinicalEventJpaRepository} lee de
 * {@code v_clinical_event}, una vista SQL que hace {@code UNION ALL} de nueve
 * tablas fuente con sus JOINs a los catálogos de tipo. Ese SQL —el filtro por
 * tenant, el orden con desempate por {@code sourceId}, la paginación sobre una
 * vista sin colecciones, el {@code LIKE} case-insensitive de {@code q}, el
 * agrupado de {@code countByType}— no lo ejercita ningún mock: solo lo ve el
 * motor real. Cubre solo CONSULTATION y SURGERY (dos de las nueve ramas del
 * {@code UNION ALL}): el mapeo campo a campo de cada tipo fuente ya lo prueba
 * {@code JpaClinicalEventDetailQueryPortTest} a nivel unitario; aquí lo que se
 * verifica es que la vista y el puente JPA funcionan de verdad, no que las
 * nueve ramas estén repetidas.
 */
@Import({JpaClinicalEventRepository.class, ClinicalEventJpaMapper.class})
@DisplayName("JpaClinicalEventRepository — la historia clínica contra la vista real")
class ClinicalEventPersistenceIT extends AbstractDataJpaTest {

    private static final Long COMPANY = SchemaSeed.COMPANY_ID;
    private static final Long OTRA_COMPANY = SchemaSeed.OTRA_COMPANY_ID;

    private static final Long SPECIE_ID = 960L;
    private static final Long BREED_ID = 961L;
    private static final Long COLOR_ID = 962L;
    private static final Long OWNER_ID = 963L;
    private static final Long ANIMAL_ID = 964L;
    private static final Long OTRO_ANIMAL_ID = 965L;
    private static final Long CONSULTATION_TYPE_ID = 966L;
    private static final Long SURGERY_TYPE_ID = 967L;

    private static final Long CONSULTATION_1 = 970L;
    private static final Long CONSULTATION_2 = 971L;
    private static final Long SURGERY_1 = 980L;

    @Autowired
    private JpaClinicalEventRepository repository;

    @PersistenceContext
    private EntityManager entityManager;

    @BeforeEach
    void sembrarLaCadenaCompleta() {
        SchemaSeed.seed(entityManager);
        insert("""
                INSERT INTO species (id, name) VALUES (%d, 'Perro')
                """.formatted(SPECIE_ID));
        insert("""
                INSERT INTO breeds (id, name, specie_id) VALUES (%d, 'Labrador', %d)
                """.formatted(BREED_ID, SPECIE_ID));
        insert("""
                INSERT INTO animal_colors (id, name, specie_id) VALUES (%d, 'Negro', %d)
                """.formatted(COLOR_ID, SPECIE_ID));
        insert("""
                INSERT INTO owners (id, name, document, document_type, person_type, city_id,
                                    company_id)
                VALUES (%d, 'Ana Ruiz', 'CC-1020', 'CEDULA_CIUDADANIA', 'NATURAL', %d, %d)
                """.formatted(OWNER_ID, SchemaSeed.CITY_ID, COMPANY));
        insert("""
                INSERT INTO animals (id, name, code, specie_id, breed_id, owner_id, gender,
                                     weight_type, animal_type, reproductive_state, color_id,
                                     company_id)
                VALUES (%d, 'Firulais', 'A-001', %d, %d, %d, 'MALE', 'KILOGRAMS', 'NONE',
                        'STERILIZED', %d, %d)
                """.formatted(ANIMAL_ID, SPECIE_ID, BREED_ID, OWNER_ID, COLOR_ID, COMPANY));
        insert("""
                INSERT INTO animals (id, name, code, specie_id, breed_id, owner_id, gender,
                                     weight_type, animal_type, reproductive_state, color_id,
                                     company_id)
                VALUES (%d, 'Michi', 'A-002', %d, %d, %d, 'FEMALE', 'KILOGRAMS', 'NONE',
                        'STERILIZED', %d, %d)
                """.formatted(OTRO_ANIMAL_ID, SPECIE_ID, BREED_ID, OWNER_ID, COLOR_ID, COMPANY));
        insert("""
                INSERT INTO consultation_types (id, name, description)
                VALUES (%d, 'Control General', 'Consulta de control')
                """.formatted(CONSULTATION_TYPE_ID));
        insert("""
                INSERT INTO surgery_types (id, name, description)
                VALUES (%d, 'Esterilización', 'Cirugía de esterilización')
                """.formatted(SURGERY_TYPE_ID));

        insert("""
                INSERT INTO consultations (id, date, consultation_type_id, anamnesis, animal_id,
                                           company_id)
                VALUES (%d, '2026-08-01', %d, 'Chequeo anual', %d, %d)
                """.formatted(CONSULTATION_1, CONSULTATION_TYPE_ID, ANIMAL_ID, COMPANY));
        insert("""
                INSERT INTO consultations (id, date, consultation_type_id, anamnesis, animal_id,
                                           company_id)
                VALUES (%d, '2026-08-10', %d, 'Vomito ocasional', %d, %d)
                """.formatted(CONSULTATION_2, CONSULTATION_TYPE_ID, ANIMAL_ID, COMPANY));
        insert("""
                INSERT INTO surgeries (id, date, surgery_type_id, description, animal_id,
                                       consultation_id, company_id)
                VALUES (%d, '2026-08-05', %d, 'Esterilización de rutina', %d, %d, %d)
                """.formatted(SURGERY_1, SURGERY_TYPE_ID, ANIMAL_ID, CONSULTATION_1, COMPANY));

        entityManager.flush();
        entityManager.clear();
    }

    private void insert(String sql) {
        entityManager.createNativeQuery(sql).executeUpdate();
    }

    private static GetClinicalHistoryQuery query(Long animalId, Long companyId,
            List<ClinicalEventType> types, LocalDate from, LocalDate to, String q,
            Long consultationId) {
        return new GetClinicalHistoryQuery(animalId, companyId, types, from, to, q, consultationId);
    }

    @Nested
    @DisplayName("findHistory — historia completa sin paginar (export a PDF)")
    class FindHistory {

        @Test
        @DisplayName("trae los eventos del animal ordenados por fecha desc, sourceId desc")
        void trae_los_eventos_ordenados_por_fecha_desc() {
            List<ClinicalEvent> historia = repository
                    .findHistory(query(ANIMAL_ID, COMPANY, List.of(), null, null, null, null));

            assertThat(historia).extracting(ClinicalEvent::sourceId).containsExactly(CONSULTATION_2,
                    SURGERY_1, CONSULTATION_1);
        }

        @Test
        @DisplayName("mapea tipo y resumen (nombre del catálogo) de cada rama del UNION")
        void mapea_tipo_y_resumen_de_cada_rama() {
            List<ClinicalEvent> historia = repository
                    .findHistory(query(ANIMAL_ID, COMPANY, List.of(), null, null, null, null));

            assertThat(historia).filteredOn(e -> e.sourceId().equals(SURGERY_1)).singleElement()
                    .satisfies(e -> {
                        assertThat(e.eventType()).isEqualTo(ClinicalEventType.SURGERY);
                        assertThat(e.summary()).isEqualTo("Esterilización");
                        assertThat(e.consultationId()).isEqualTo(CONSULTATION_1);
                    });
        }

        @Test
        @DisplayName("filtra por tipo cuando se piden solo CONSULTATION")
        void filtra_por_tipo() {
            List<ClinicalEvent> soloConsultas = repository.findHistory(query(ANIMAL_ID, COMPANY,
                    List.of(ClinicalEventType.CONSULTATION), null, null, null, null));

            assertThat(soloConsultas).extracting(ClinicalEvent::eventType)
                    .containsOnly(ClinicalEventType.CONSULTATION);
            assertThat(soloConsultas).hasSize(2);
        }

        @Test
        @DisplayName("filtra por rango de fechas")
        void filtra_por_rango_de_fechas() {
            List<ClinicalEvent> enRango = repository.findHistory(query(ANIMAL_ID, COMPANY,
                    List.of(), LocalDate.of(2026, 8, 6), LocalDate.of(2026, 8, 31), null, null));

            assertThat(enRango).extracting(ClinicalEvent::sourceId).containsExactly(CONSULTATION_2);
        }

        @Test
        @DisplayName("filtra por texto libre sobre el resumen, sin distinguir mayúsculas")
        void filtra_por_texto_libre_sin_distinguir_mayusculas() {
            List<ClinicalEvent> filtrados = repository.findHistory(
                    query(ANIMAL_ID, COMPANY, List.of(), null, null, "esteriliza", null));

            assertThat(filtrados).extracting(ClinicalEvent::sourceId).containsExactly(SURGERY_1);
        }

        @Test
        @DisplayName("filtra por consultationId a los procedimientos derivados de esa consulta")
        void filtra_por_consultation_id() {
            List<ClinicalEvent> derivados = repository.findHistory(
                    query(ANIMAL_ID, COMPANY, List.of(), null, null, null, CONSULTATION_1));

            assertThat(derivados).extracting(ClinicalEvent::sourceId).containsExactly(SURGERY_1);
        }

        @Test
        @DisplayName("un animal sin eventos devuelve lista vacía")
        void animal_sin_eventos_devuelve_vacio() {
            assertThat(repository
                    .findHistory(query(OTRO_ANIMAL_ID, COMPANY, List.of(), null, null, null, null)))
                    .isEmpty();
        }

        @Test
        @DisplayName("no trae eventos de otra empresa aunque el animalId coincida")
        void no_trae_eventos_de_otra_empresa() {
            assertThat(repository
                    .findHistory(query(ANIMAL_ID, OTRA_COMPANY, List.of(), null, null, null, null)))
                    .isEmpty();
        }
    }

    @Nested
    @DisplayName("findHistoryPage — la misma historia, paginada")
    class FindHistoryPage {

        @Test
        @DisplayName("pagina respetando el mismo orden y trae los totales de la consulta")
        void pagina_respetando_el_orden_y_los_totales() {
            PageResult<ClinicalEvent> pagina1 = repository.findHistoryPage(
                    query(ANIMAL_ID, COMPANY, List.of(), null, null, null, null), 0, 2);

            assertThat(pagina1.content()).extracting(ClinicalEvent::sourceId)
                    .containsExactly(CONSULTATION_2, SURGERY_1);
            assertThat(pagina1.totalElements()).isEqualTo(3L);
            assertThat(pagina1.totalPages()).isEqualTo(2);

            PageResult<ClinicalEvent> pagina2 = repository.findHistoryPage(
                    query(ANIMAL_ID, COMPANY, List.of(), null, null, null, null), 1, 2);

            assertThat(pagina2.content()).extracting(ClinicalEvent::sourceId)
                    .containsExactly(CONSULTATION_1);
        }

        @Test
        @DisplayName("una página fuera de rango no es un error, viene vacía")
        void pagina_fuera_de_rango_viene_vacia() {
            PageResult<ClinicalEvent> pagina = repository.findHistoryPage(
                    query(ANIMAL_ID, COMPANY, List.of(), null, null, null, null), 5, 20);

            assertThat(pagina.content()).isEmpty();
            assertThat(pagina.totalElements()).isEqualTo(3L);
        }
    }

    @Nested
    @DisplayName("countByType — chips de resumen por tipo")
    class CountByType {

        @Test
        @DisplayName("cuenta los eventos del animal agrupados por tipo, sin filtrar por rango")
        void cuenta_agrupado_por_tipo() {
            List<ClinicalEventTypeCountDto> conteos = repository.countByType(ANIMAL_ID, COMPANY);

            assertThat(conteos).extracting(ClinicalEventTypeCountDto::eventType)
                    .containsExactlyInAnyOrder(ClinicalEventType.CONSULTATION,
                            ClinicalEventType.SURGERY);
            assertThat(conteos).filteredOn(c -> c.eventType() == ClinicalEventType.CONSULTATION)
                    .singleElement().extracting(ClinicalEventTypeCountDto::count).isEqualTo(2L);
        }

        @Test
        @DisplayName("un animal de otra empresa no contamina el conteo")
        void otra_empresa_no_contamina_el_conteo() {
            assertThat(repository.countByType(ANIMAL_ID, OTRA_COMPANY)).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByCompany — historia de toda la empresa, para el calendario")
    class FindByCompany {

        @Test
        @DisplayName("trae los eventos de todos los animales de la empresa, orden ascendente")
        void trae_los_eventos_de_toda_la_empresa_orden_ascendente() {
            List<ClinicalEvent> historia = repository
                    .findByCompany(new ListCompanyClinicalEventsQuery(COMPANY, List.of(),
                            LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31)));

            assertThat(historia).extracting(ClinicalEvent::sourceId).containsExactly(CONSULTATION_1,
                    SURGERY_1, CONSULTATION_2);
        }

        @Test
        @DisplayName("no cruza eventos de otra empresa")
        void no_cruza_eventos_de_otra_empresa() {
            List<ClinicalEvent> historia = repository
                    .findByCompany(new ListCompanyClinicalEventsQuery(OTRA_COMPANY, List.of(),
                            LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31)));

            assertThat(historia).isEmpty();
        }

        @Test
        @DisplayName("filtra por rango de fechas de toda la empresa")
        void filtra_por_rango_de_fechas() {
            List<ClinicalEvent> historia = repository
                    .findByCompany(new ListCompanyClinicalEventsQuery(COMPANY, List.of(),
                            LocalDate.of(2026, 8, 2), LocalDate.of(2026, 8, 6)));

            assertThat(historia).extracting(ClinicalEvent::sourceId).containsExactly(SURGERY_1);
        }
    }
}
