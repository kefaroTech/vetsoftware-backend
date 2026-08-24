package com.vetsoftware.app.configurator.infrastructure.persistence;

import com.vetsoftware.app.configurator.domain.EffectType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface ConfiguratorEffectJpaRepository
        extends
            JpaRepository<ConfiguratorEffectJpaEntity, Long> {

    /**
     * Por {@code id} ascendente, que es el orden de aplicación del resolvedor. No
     * es una preferencia del adaptador: los efectos no conmutan, así que este orden
     * es parte del contrato de la resolución.
     */
    List<ConfiguratorEffectJpaEntity> findAllByOrderByIdAsc();

    boolean existsByOptionId(Long optionId);

    boolean existsByQuestionId(Long questionId);

    /**
     * Los efectos <em>activos</em> de un tipo concreto disparados por una pregunta.
     * Es lo que necesita el otro lado de la invariante de
     * {@code QUANTITY_FROM_ANSWER}: cambiar el {@code answerType} de la pregunta
     * tiene que mirar si algún efecto vivo depende de que siga siendo
     * {@code NUMBER}. Derivada y no nativa a propósito — aquí el
     * {@code @SQLRestriction} es exactamente lo que se quiere: un efecto dado de
     * baja no se dispara, así que no bloquea nada.
     */
    boolean existsByQuestionIdAndEffect(Long questionId, EffectType effect);

    /**
     * Ignora el borrado lógico: {@code uq_configurator_effects_option} tampoco lo
     * ignora.
     *
     * <p>
     * El {@code effect} entra como {@code String} porque la consulta es nativa y la
     * columna guarda el nombre del enum ({@code @Enumerated(STRING)}); pasar el
     * enum dejaría que el driver lo bindara por ordinal.
     */
    @Query(value = """
            SELECT id FROM configurator_effects
            WHERE option_id = :optionId AND catalog_item_id = :catalogItemId AND effect = :effect
            """, nativeQuery = true)
    Optional<Long> findAnyIdByOptionTrigger(@Param("optionId") Long optionId,
            @Param("catalogItemId") Long catalogItemId, @Param("effect") String effect);

    /** {@code long} y no {@code boolean}: literal booleano proyectado, #196. */
    @Query(value = """
            SELECT COUNT(*) FROM configurator_effects
            WHERE option_id = :optionId AND catalog_item_id = :catalogItemId AND effect = :effect
              AND enabled = TRUE
            """, nativeQuery = true)
    long countEnabledByOptionTrigger(@Param("optionId") Long optionId,
            @Param("catalogItemId") Long catalogItemId, @Param("effect") String effect);

    /**
     * La gemela por pregunta: {@code uq_configurator_effects_question}. Hacen falta
     * las dos porque MySQL admite múltiples {@code NULL} en un índice único, así
     * que cada una cubre exactamente el conjunto de filas que la otra deja pasar.
     */
    @Query(value = """
            SELECT id FROM configurator_effects
            WHERE question_id = :questionId AND catalog_item_id = :catalogItemId
              AND effect = :effect
            """, nativeQuery = true)
    Optional<Long> findAnyIdByQuestionTrigger(@Param("questionId") Long questionId,
            @Param("catalogItemId") Long catalogItemId, @Param("effect") String effect);

    /** {@code long} y no {@code boolean}: literal booleano proyectado, #196. */
    @Query(value = """
            SELECT COUNT(*) FROM configurator_effects
            WHERE question_id = :questionId AND catalog_item_id = :catalogItemId
              AND effect = :effect AND enabled = TRUE
            """, nativeQuery = true)
    long countEnabledByQuestionTrigger(@Param("questionId") Long questionId,
            @Param("catalogItemId") Long catalogItemId, @Param("effect") String effect);

    /**
     * Con {@code version = version + 1} en el {@code SET}: la tabla está versionada
     * ({@code UPDATE_MASIVO_MUEVE_LA_VERSION}, #53). La versión no va en el
     * {@code WHERE} — ver el javadoc gemelo en
     * {@code ConfiguratorQuestionJpaRepository}.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query(value = """
            UPDATE configurator_effects
            SET enabled = TRUE, version = version + 1
            WHERE id = :id
            """, nativeQuery = true)
    int reactivate(@Param("id") Long id);
}
