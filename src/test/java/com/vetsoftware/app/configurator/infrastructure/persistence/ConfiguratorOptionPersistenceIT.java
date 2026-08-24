package com.vetsoftware.app.configurator.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.catalogitem.testsupport.CatalogItemMother;
import com.vetsoftware.app.configurator.domain.AnswerType;
import com.vetsoftware.app.configurator.domain.ConfiguratorOption;
import com.vetsoftware.app.configurator.domain.ConfiguratorQuestion;
import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import com.vetsoftware.app.testsupport.PersistenceSliceConfig;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

@Import(PersistenceSliceConfig.class)
@DisplayName("JpaConfiguratorOptionRepository — opciones por pregunta contra MySQL real")
class ConfiguratorOptionPersistenceIT extends AbstractDataJpaTest {

    @Autowired
    private JpaConfiguratorQuestionRepository questions;
    @Autowired
    private JpaConfiguratorOptionRepository repository;
    @PersistenceContext
    private EntityManager entityManager;

    @Test
    @DisplayName("acota por questionId y ordena opciones por sortOrder")
    void acota_por_question_id_y_ordena_por_sort_order() {
        ConfiguratorQuestion question = questions
                .save(ConfiguratorQuestion.create("HAS_LAB", "¿Tiene laboratorio?", null,
                        AnswerType.BOOLEAN, null, true, 0, CatalogItemMother.RELOJ));
        repository.save(ConfiguratorOption.create(question.getId(), "NO", "No", null, 20,
                CatalogItemMother.RELOJ));
        ConfiguratorOption si = repository.save(ConfiguratorOption.create(question.getId(), "YES",
                "Sí", "Laboratorio propio", 10, CatalogItemMother.RELOJ));
        entityManager.flush();
        entityManager.clear();

        assertThat(repository.findByQuestionId(question.getId()))
                .extracting(ConfiguratorOption::getCode).containsExactly("YES", "NO");
        assertThat(repository.findById(si.getId())).get().extracting(ConfiguratorOption::getLabel)
                .isEqualTo("Sí");
        assertThat(repository.existsByQuestionIdAndCode(question.getId(), "YES")).isTrue();
    }

    @Test
    @DisplayName("el listado global ordena por pregunta, orden y id, que es lo que arma el cuestionario")
    void el_listado_global_ordena_por_pregunta_orden_e_id() {
        ConfiguratorQuestion primera = questions.save(ConfiguratorQuestion.create("Q_ORDEN_A",
                "¿A?", null, AnswerType.SINGLE, null, true, 0, CatalogItemMother.RELOJ));
        ConfiguratorQuestion segunda = questions.save(ConfiguratorQuestion.create("Q_ORDEN_B",
                "¿B?", null, AnswerType.SINGLE, null, true, 1, CatalogItemMother.RELOJ));
        repository.save(ConfiguratorOption.create(segunda.getId(), "B1", "B1", null, 0,
                CatalogItemMother.RELOJ));
        repository.save(ConfiguratorOption.create(primera.getId(), "A2", "A2", null, 5,
                CatalogItemMother.RELOJ));
        repository.save(ConfiguratorOption.create(primera.getId(), "A1", "A1", null, 1,
                CatalogItemMother.RELOJ));
        entityManager.flush();
        entityManager.clear();

        assertThat(repository.findAllOrdered()).extracting(ConfiguratorOption::getCode)
                .containsSubsequence("A1", "A2", "B1");
        assertThat(repository.existsByQuestionId(primera.getId())).isTrue();
    }

    @Test
    @DisplayName("la baja logica retira la opcion del cuestionario y del listado de su pregunta")
    void la_baja_logica_retira_la_opcion_del_cuestionario() {
        ConfiguratorQuestion question = questions.save(ConfiguratorQuestion.create("Q_BAJA",
                "¿Baja?", null, AnswerType.SINGLE, null, true, 0, CatalogItemMother.RELOJ));
        ConfiguratorOption opcion = repository.save(ConfiguratorOption.create(question.getId(),
                "YES", "Sí", null, 0, CatalogItemMother.RELOJ));
        entityManager.flush();

        repository.delete(opcion.getId());
        entityManager.flush();
        entityManager.clear();

        assertThat(repository.findById(opcion.getId())).isEmpty();
        assertThat(repository.findByQuestionId(question.getId())).isEmpty();
        assertThat(repository.existsByQuestionId(question.getId())).isFalse();
    }

    /**
     * <strong>El defecto que este test deja escrito.</strong>
     * {@code uq_configurator_options_code} es {@code (question_id, code)} y
     * <em>no</em> incluye {@code enabled}, así que una opción dada de baja
     * <strong>sigue ocupando su código</strong>. Pero
     * {@code existsByQuestionIdAndCode} es una consulta derivada sobre una entidad
     * con {@code @SQLRestriction("enabled = true")}: no ve la fila y responde que
     * el código está libre.
     *
     * <p>
     * La consecuencia es que {@code CreateConfiguratorOptionService} pasa su
     * guardia y ejecuta un {@code INSERT} que la clave única rechaza.
     * {@code GlobalExceptionHandler.handleDataIntegrity} no tiene mapeo para esta
     * constraint, así que cae en su rama genérica: un 409
     * {@code DATA_INTEGRITY_VIOLATION} con el detalle «Database constraint
     * violation», que no dice qué corregir, sobre una fila que el administrador no
     * puede ver ni listar. Las tres tablas puente de {@code catalogitem} resuelven
     * el mismo caso reactivando, con un {@code findAnyByPair} nativo.
     *
     * <p>
     * El test afirma los dos hechos por separado: que la guardia dice «libre» y que
     * la fila sigue ahí. No afirma el 500 porque el arreglo correcto no es
     * documentarlo, sino que la guardia deje de ser ciega.
     */
    @Test
    @DisplayName("una opcion dada de baja sigue ocupando su codigo, pero la guardia de alta no la ve")
    void una_opcion_de_baja_sigue_ocupando_su_codigo_y_la_guardia_no_la_ve() {
        ConfiguratorQuestion question = questions.save(ConfiguratorQuestion.create("Q_CODIGO",
                "¿Código?", null, AnswerType.SINGLE, null, true, 0, CatalogItemMother.RELOJ));
        ConfiguratorOption opcion = repository.save(ConfiguratorOption.create(question.getId(),
                "YES", "Sí", null, 0, CatalogItemMother.RELOJ));
        entityManager.flush();

        repository.delete(opcion.getId());
        entityManager.flush();
        entityManager.clear();

        // La guardia del alta cree que el codigo quedo libre...
        assertThat(repository.existsByQuestionIdAndCode(question.getId(), "YES")).isFalse();

        // ...y la fila sigue ahi, ocupando uq_configurator_options_code.
        Number filas = (Number) entityManager.createNativeQuery("""
                SELECT COUNT(*) FROM configurator_options
                WHERE question_id = :questionId AND code = :code
                """).setParameter("questionId", question.getId()).setParameter("code", "YES")
                .getSingleResult();
        assertThat(filas.longValue()).isEqualTo(1L);
    }
}
