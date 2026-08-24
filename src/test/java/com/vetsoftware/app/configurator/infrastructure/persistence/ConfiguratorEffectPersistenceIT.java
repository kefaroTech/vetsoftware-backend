package com.vetsoftware.app.configurator.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.catalogitem.testsupport.CatalogItemMother;
import com.vetsoftware.app.configurator.domain.AnswerType;
import com.vetsoftware.app.configurator.domain.ConfiguratorEffect;
import com.vetsoftware.app.configurator.domain.ConfiguratorOption;
import com.vetsoftware.app.configurator.domain.ConfiguratorQuestion;
import com.vetsoftware.app.configurator.domain.EffectType;
import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import com.vetsoftware.app.testsupport.PersistenceSliceConfig;
import com.vetsoftware.app.testsupport.SchemaSeed;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

@Import(PersistenceSliceConfig.class)
@DisplayName("JpaConfiguratorEffectRepository — efectos comerciales contra MySQL real")
class ConfiguratorEffectPersistenceIT extends AbstractDataJpaTest {

    @Autowired
    private JpaConfiguratorQuestionRepository questions;
    @Autowired
    private JpaConfiguratorOptionRepository options;
    @Autowired
    private JpaConfiguratorEffectRepository repository;
    @PersistenceContext
    private EntityManager entityManager;

    @BeforeEach
    void seed() {
        SchemaSeed.seed(entityManager);
    }

    @Test
    @DisplayName("persiste exactamente un disparador y conserva el artículo afectado")
    void persiste_un_disparador_y_conserva_el_articulo() {
        ConfiguratorQuestion question = questions
                .save(ConfiguratorQuestion.create("USERS", "¿Cuántos usuarios?", null,
                        AnswerType.NUMBER, null, true, 0, CatalogItemMother.RELOJ));
        ConfiguratorEffect guardado = repository.save(
                ConfiguratorEffect.create(null, question.getId(), SchemaSeed.CATALOG_ITEM_CORE_ID,
                        EffectType.QUANTITY_FROM_ANSWER, null, CatalogItemMother.RELOJ));
        entityManager.flush();
        entityManager.clear();

        assertThat(repository.findById(guardado.getId())).get().satisfies(leido -> {
            assertThat(leido.getQuestionId()).isEqualTo(question.getId());
            assertThat(leido.getOptionId()).isNull();
            assertThat(leido.getCatalogItemId()).isEqualTo(SchemaSeed.CATALOG_ITEM_CORE_ID);
            assertThat(leido.getEffect()).isEqualTo(EffectType.QUANTITY_FROM_ANSWER);
        });
        assertThat(repository.existsByQuestionId(question.getId())).isTrue();
    }

    /**
     * El orden de los efectos es parte del contrato de
     * {@code ConfiguratorResolver}: {@code ADD} y {@code REMOVE} sobre el mismo
     * artículo no conmutan. El resolvedor vuelve a ordenar por id en memoria, pero
     * la consulta también lo hace, y esto comprueba que el {@code ORDER BY} llega
     * de verdad al SQL en vez de depender del orden en que MySQL devuelva las
     * filas.
     */
    @Test
    @DisplayName("findAllOrdered devuelve los efectos por id ascendente, que es el orden del contrato")
    void find_all_ordered_devuelve_los_efectos_por_id_ascendente() {
        ConfiguratorQuestion question = questions.save(ConfiguratorQuestion.create("ORDEN",
                "¿Cuántos?", null, AnswerType.NUMBER, null, true, 0, CatalogItemMother.RELOJ));
        ConfiguratorEffect primero = repository.save(
                ConfiguratorEffect.create(null, question.getId(), SchemaSeed.CATALOG_ITEM_CORE_ID,
                        EffectType.ADD, null, CatalogItemMother.RELOJ));
        ConfiguratorEffect segundo = repository.save(
                ConfiguratorEffect.create(null, question.getId(), SchemaSeed.CATALOG_ITEM_CORE_ID,
                        EffectType.REMOVE, null, CatalogItemMother.RELOJ));
        entityManager.flush();
        entityManager.clear();

        assertThat(repository.findAllOrdered()).extracting(ConfiguratorEffect::getId)
                .containsSubsequence(primero.getId(), segundo.getId());
        assertThat(repository.findAllOrdered())
                .isSortedAccordingTo(java.util.Comparator.comparing(ConfiguratorEffect::getId));
    }

    @Test
    @DisplayName("el listado paginado conserva los totales de la consulta, no los del contenido")
    void el_listado_paginado_conserva_los_totales_de_la_consulta() {
        ConfiguratorQuestion question = questions.save(ConfiguratorQuestion.create("PAGINA",
                "¿Cuántos?", null, AnswerType.NUMBER, null, true, 0, CatalogItemMother.RELOJ));
        repository.save(ConfiguratorEffect.create(null, question.getId(),
                SchemaSeed.CATALOG_ITEM_CORE_ID, EffectType.ADD, null, CatalogItemMother.RELOJ));
        entityManager.flush();
        entityManager.clear();

        assertThat(repository.findAll(0, 20)).satisfies(pagina -> {
            assertThat(pagina.page()).isZero();
            assertThat(pagina.pageSize()).isEqualTo(20);
            assertThat(pagina.totalElements()).isPositive();
            assertThat(pagina.content()).isNotEmpty();
        });
    }

    /**
     * Un efecto dado de baja no puede volver a disparar: el resolvedor lo filtra
     * con {@code isEnabled()}, pero la primera red es que la consulta ni siquiera
     * lo traiga.
     */
    @Test
    @DisplayName("un efecto dado de baja desaparece de la consulta que alimenta al resolvedor")
    void un_efecto_de_baja_desaparece_de_la_consulta_del_resolvedor() {
        ConfiguratorQuestion question = questions.save(ConfiguratorQuestion.create("BAJA_EFECTO",
                "¿Cuántos?", null, AnswerType.NUMBER, null, true, 0, CatalogItemMother.RELOJ));
        ConfiguratorEffect efecto = repository.save(
                ConfiguratorEffect.create(null, question.getId(), SchemaSeed.CATALOG_ITEM_CORE_ID,
                        EffectType.ADD, null, CatalogItemMother.RELOJ));
        entityManager.flush();

        repository.delete(efecto.getId());
        entityManager.flush();
        entityManager.clear();

        assertThat(repository.findById(efecto.getId())).isEmpty();
        assertThat(repository.findAllOrdered()).extracting(ConfiguratorEffect::getId)
                .doesNotContain(efecto.getId());
        assertThat(repository.existsByQuestionId(question.getId())).isFalse();
    }

    @Test
    @DisplayName("existsByOptionId distingue el disparador por opcion del disparador por pregunta")
    void exists_by_option_id_distingue_el_disparador() {
        ConfiguratorQuestion question = questions.save(ConfiguratorQuestion.create("POR_OPCION",
                "¿Vende?", null, AnswerType.SINGLE, null, true, 0, CatalogItemMother.RELOJ));
        ConfiguratorOption opcion = options.save(ConfiguratorOption.create(question.getId(), "YES",
                "Sí", null, 0, CatalogItemMother.RELOJ));
        repository.save(ConfiguratorEffect.create(opcion.getId(), null,
                SchemaSeed.CATALOG_ITEM_CORE_ID, EffectType.ADD, null, CatalogItemMother.RELOJ));
        entityManager.flush();
        entityManager.clear();

        assertThat(repository.existsByOptionId(opcion.getId())).isTrue();
        assertThat(repository.existsByQuestionId(question.getId())).isFalse();
        assertThat(repository.existsByOptionId(999_999L)).isFalse();
    }
}
