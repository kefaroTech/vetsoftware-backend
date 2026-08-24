package com.vetsoftware.app.configurator.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.catalogitem.testsupport.CatalogItemMother;
import com.vetsoftware.app.configurator.domain.AnswerType;
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
@DisplayName("JpaConfiguratorQuestionRepository — preguntas configurables contra MySQL real")
class ConfiguratorQuestionPersistenceIT extends AbstractDataJpaTest {

    @Autowired
    private JpaConfiguratorQuestionRepository repository;
    @PersistenceContext
    private EntityManager entityManager;

    @Test
    @DisplayName("guarda la pregunta y respeta sortOrder al listar")
    void guarda_la_pregunta_y_respeta_sort_order() {
        repository.save(ConfiguratorQuestion.create("SIZE", "¿Cuántos usuarios?", null,
                AnswerType.NUMBER, null, true, 20, CatalogItemMother.RELOJ));
        ConfiguratorQuestion primero = repository.save(ConfiguratorQuestion.create("HAS_LAB",
                "¿Tiene laboratorio?", "Incluye equipos propios", AnswerType.BOOLEAN, null, false,
                10, CatalogItemMother.RELOJ));
        entityManager.flush();
        entityManager.clear();

        assertThat(repository.findAllOrdered()).extracting(ConfiguratorQuestion::getCode)
                .containsExactly("HAS_LAB", "SIZE");
        assertThat(repository.findById(primero.getId())).get().satisfies(leida -> {
            assertThat(leida.getQuestionText()).isEqualTo("¿Tiene laboratorio?");
            assertThat(leida.getAnswerType()).isEqualTo(AnswerType.BOOLEAN);
        });
    }

    @Test
    @DisplayName("el listado paginado conserva los totales de la consulta")
    void el_listado_paginado_conserva_los_totales_de_la_consulta() {
        repository.save(ConfiguratorQuestion.create("PAGINA_Q", "¿Cuántos?", null,
                AnswerType.NUMBER, null, true, 30, CatalogItemMother.RELOJ));
        entityManager.flush();
        entityManager.clear();

        assertThat(repository.findAll(0, 20)).satisfies(pagina -> {
            assertThat(pagina.page()).isZero();
            assertThat(pagina.pageSize()).isEqualTo(20);
            assertThat(pagina.totalElements()).isPositive();
            assertThat(pagina.content()).isNotEmpty();
        });
    }

    @Test
    @DisplayName("la baja logica retira la pregunta del cuestionario y de la consulta por id")
    void la_baja_logica_retira_la_pregunta_del_cuestionario() {
        ConfiguratorQuestion guardada = repository.save(ConfiguratorQuestion.create("BAJA_Q",
                "¿Baja?", null, AnswerType.SINGLE, null, true, 40, CatalogItemMother.RELOJ));
        entityManager.flush();

        repository.delete(guardada.getId());
        entityManager.flush();
        entityManager.clear();

        assertThat(repository.findById(guardada.getId())).isEmpty();
        assertThat(repository.findAllOrdered()).extracting(ConfiguratorQuestion::getCode)
                .doesNotContain("BAJA_Q");
    }

    /**
     * El mismo defecto que en {@code configurator_options}, un nivel más arriba:
     * {@code uq_configurator_questions_code} es {@code (code)} y no incluye
     * {@code enabled}, mientras {@code existsByCode} solo ve las preguntas activas.
     * Volver a dar de alta el código de una pregunta retirada pasa la guardia y
     * muere contra la clave única.
     */
    @Test
    @DisplayName("una pregunta dada de baja sigue ocupando su codigo, pero la guardia de alta no la ve")
    void una_pregunta_de_baja_sigue_ocupando_su_codigo_y_la_guardia_no_la_ve() {
        ConfiguratorQuestion guardada = repository.save(ConfiguratorQuestion.create("CODIGO_Q",
                "¿Código?", null, AnswerType.SINGLE, null, true, 41, CatalogItemMother.RELOJ));
        entityManager.flush();
        assertThat(repository.existsByCode("CODIGO_Q")).isTrue();

        repository.delete(guardada.getId());
        entityManager.flush();
        entityManager.clear();

        assertThat(repository.existsByCode("CODIGO_Q")).isFalse();

        Number filas = (Number) entityManager
                .createNativeQuery("SELECT COUNT(*) FROM configurator_questions WHERE code = :code")
                .setParameter("code", "CODIGO_Q").getSingleResult();
        assertThat(filas.longValue()).isEqualTo(1L);
    }

    @Test
    @DisplayName("existsByParentOptionId ve las preguntas que cuelgan de una opcion")
    void exists_by_parent_option_id_ve_las_preguntas_que_cuelgan() {
        assertThat(repository.existsByParentOptionId(999_999L)).isFalse();
    }
}
