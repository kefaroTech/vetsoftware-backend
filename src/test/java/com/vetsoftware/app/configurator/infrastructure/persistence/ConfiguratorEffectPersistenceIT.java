package com.vetsoftware.app.configurator.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.catalogitem.testsupport.CatalogItemMother;
import com.vetsoftware.app.configurator.domain.AnswerType;
import com.vetsoftware.app.configurator.domain.ConfiguratorEffect;
import com.vetsoftware.app.configurator.domain.ConfiguratorOption;
import com.vetsoftware.app.configurator.domain.ConfiguratorQuestion;
import com.vetsoftware.app.configurator.domain.EffectType;
import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import com.vetsoftware.app.testsupport.EngineConstraint;
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

    /** Resuelto, no sembrado: el articulo CORE llega del changeset 308. */
    private Long nucleo;

    @BeforeEach
    void seed() {
        SchemaSeed.seed(entityManager);
        nucleo = SchemaSeed.catalogItemId(entityManager, "CORE");
    }

    @Test
    @DisplayName("persiste exactamente un disparador y conserva el artículo afectado")
    void persiste_un_disparador_y_conserva_el_articulo() {
        ConfiguratorQuestion question = questions
                .save(ConfiguratorQuestion.create("USERS", "¿Cuántos usuarios?", null,
                        AnswerType.NUMBER, null, true, 0, CatalogItemMother.RELOJ));
        ConfiguratorEffect guardado = repository
                .save(ConfiguratorEffect.create(null, question.getId(), nucleo,
                        EffectType.QUANTITY_FROM_ANSWER, null, CatalogItemMother.RELOJ));
        entityManager.flush();
        entityManager.clear();

        assertThat(repository.findById(guardado.getId())).get().satisfies(leido -> {
            assertThat(leido.getQuestionId()).isEqualTo(question.getId());
            assertThat(leido.getOptionId()).isNull();
            assertThat(leido.getCatalogItemId()).isEqualTo(nucleo);
            assertThat(leido.getEffect()).isEqualTo(EffectType.QUANTITY_FROM_ANSWER);
        });
        assertThat(repository.existsByQuestionId(question.getId())).isTrue();
    }

    /**
     * El orden de los efectos es parte del contrato de
     * {@code ConfiguratorResolver}: {@code ADD} y {@code REMOVE} sobre el mismo
     * artículo no conmutan. El resolvedor vuelve a ordenar en memoria, pero la
     * consulta también lo hace, y esto comprueba que el {@code ORDER BY} llega de
     * verdad al SQL en vez de depender del orden en que MySQL devuelva las filas.
     *
     * <p>
     * <strong>Este caso afirmaba antes «por id ascendente», que era el defecto
     * escrito como contrato.</strong> La columna {@code priority} llevaba desde el
     * changeset 238 en el esquema sin estar mapeada, así que el orden real era el
     * de inserción. Ahora los dos efectos se guardan con la prioridad
     * <em>invertida</em> respecto de sus ids: el {@code ADD} entra primero —id
     * menor— con prioridad 20 y el {@code REMOVE} después —id mayor— con prioridad
     * 10. Si alguien devolviera el {@code ORDER BY} al id, este caso se pone rojo;
     * con los ids alineados pasaría igual con el defecto vivo, que es justo por lo
     * que sobrevivió.
     */
    @Test
    @DisplayName("findAllOrdered devuelve los efectos por (priority, id), no por orden de insercion")
    void find_all_ordered_devuelve_los_efectos_por_prioridad() {
        ConfiguratorQuestion question = questions.save(ConfiguratorQuestion.create("ORDEN",
                "¿Cuántos?", null, AnswerType.NUMBER, null, true, 0, CatalogItemMother.RELOJ));
        ConfiguratorEffect add = ConfiguratorEffect.create(null, question.getId(), nucleo,
                EffectType.ADD, null, CatalogItemMother.RELOJ);
        add.reprioritize(20);
        ConfiguratorEffect addGuardado = repository.save(add);
        ConfiguratorEffect remove = ConfiguratorEffect.create(null, question.getId(), nucleo,
                EffectType.REMOVE, null, CatalogItemMother.RELOJ);
        remove.reprioritize(10);
        ConfiguratorEffect removeGuardado = repository.save(remove);
        entityManager.flush();
        entityManager.clear();

        // El id mayor sale ANTES porque su prioridad es menor.
        assertThat(repository.findAllOrdered()).extracting(ConfiguratorEffect::getId)
                .containsSubsequence(removeGuardado.getId(), addGuardado.getId());
        assertThat(repository.findAllOrdered()).isSortedAccordingTo(
                java.util.Comparator.comparingInt(ConfiguratorEffect::getPriority));
    }

    /**
     * La prioridad tiene que sobrevivir al viaje de ida y vuelta. Si el mapper se
     * la dejara, la columna volvería a valer 0 en cada guardado y el reordenado
     * sería un endpoint que contesta 200 y no cambia nada.
     */
    @Test
    @DisplayName("la prioridad sobrevive al guardado y a la relectura desde la base")
    void la_prioridad_sobrevive_al_guardado_y_a_la_relectura() {
        ConfiguratorQuestion question = questions.save(ConfiguratorQuestion.create("PRIORIDAD",
                "¿Cuántos?", null, AnswerType.NUMBER, null, true, 0, CatalogItemMother.RELOJ));
        ConfiguratorEffect efecto = ConfiguratorEffect.create(null, question.getId(), nucleo,
                EffectType.ADD, null, CatalogItemMother.RELOJ);
        efecto.reprioritize(4321);
        ConfiguratorEffect guardado = repository.save(efecto);
        entityManager.flush();
        entityManager.clear();

        assertThat(repository.findById(guardado.getId())).get()
                .extracting(ConfiguratorEffect::getPriority).isEqualTo(4321);
    }

    /**
     * {@code chk_configurator_effects_priority} acota la columna a 0..9999. El
     * dominio lo comprueba antes, así que este caso salta la validación de Java
     * escribiendo por el {@code EntityManager}: sin él, nadie prueba que la
     * barandilla del motor siga puesta.
     */
    @Test
    @DisplayName("el motor rechaza una prioridad fuera de 0..9999 aunque el dominio no la vea")
    void el_motor_rechaza_una_prioridad_fuera_de_rango() {
        ConfiguratorQuestion question = questions.save(ConfiguratorQuestion.create("RANGO",
                "¿Cuántos?", null, AnswerType.NUMBER, null, true, 0, CatalogItemMother.RELOJ));
        ConfiguratorEffectJpaEntity fila = new ConfiguratorEffectJpaEntity();
        fila.setQuestionId(question.getId());
        fila.setCatalogItemId(nucleo);
        fila.setEffect(EffectType.ADD);
        fila.setPriority(10_000);
        fila.setCreatedDate(java.time.LocalDateTime.now(CatalogItemMother.RELOJ));
        fila.setEnabled(true);

        EngineConstraint.assertViolates("chk_configurator_effects_priority", () -> {
            entityManager.persist(fila);
            entityManager.flush();
        });
    }

    @Test
    @DisplayName("el listado paginado conserva los totales de la consulta, no los del contenido")
    void el_listado_paginado_conserva_los_totales_de_la_consulta() {
        ConfiguratorQuestion question = questions.save(ConfiguratorQuestion.create("PAGINA",
                "¿Cuántos?", null, AnswerType.NUMBER, null, true, 0, CatalogItemMother.RELOJ));
        repository.save(ConfiguratorEffect.create(null, question.getId(), nucleo, EffectType.ADD,
                null, CatalogItemMother.RELOJ));
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
        ConfiguratorEffect efecto = repository.save(ConfiguratorEffect.create(null,
                question.getId(), nucleo, EffectType.ADD, null, CatalogItemMother.RELOJ));
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
        repository.save(ConfiguratorEffect.create(opcion.getId(), null, nucleo, EffectType.ADD,
                null, CatalogItemMother.RELOJ));
        entityManager.flush();
        entityManager.clear();

        assertThat(repository.existsByOptionId(opcion.getId())).isTrue();
        assertThat(repository.existsByQuestionId(question.getId())).isFalse();
        assertThat(repository.existsByOptionId(999_999L)).isFalse();
    }
}
