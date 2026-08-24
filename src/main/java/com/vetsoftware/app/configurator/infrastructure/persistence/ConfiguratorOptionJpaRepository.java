package com.vetsoftware.app.configurator.infrastructure.persistence;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface ConfiguratorOptionJpaRepository
        extends
            JpaRepository<ConfiguratorOptionJpaEntity, Long> {

    List<ConfiguratorOptionJpaEntity> findAllByOrderByQuestionIdAscSortOrderAscIdAsc();

    List<ConfiguratorOptionJpaEntity> findByQuestionIdOrderBySortOrderAscIdAsc(Long questionId);

    /**
     * Las opciones de varias preguntas en una sola consulta. Mismo alcance que la
     * de arriba -solo activas, por el {@code @SQLRestriction}-, y el mismo orden,
     * con {@code question_id} por delante para que el agrupado salga estable.
     */
    List<ConfiguratorOptionJpaEntity> findByQuestionIdInOrderByQuestionIdAscSortOrderAscIdAsc(
            Collection<Long> questionIds);

    /** Solo las activas: el {@code @SQLRestriction} de la entidad las acota. */
    boolean existsByQuestionIdAndCode(Long questionId, String code);

    boolean existsByQuestionId(Long questionId);

    /**
     * Ignora el borrado lógico: {@code uq_configurator_options_code} —que es
     * {@code (question_id, code)}— tampoco lo ignora. Nativa porque es la única
     * forma de esquivar el {@code @SQLRestriction}.
     */
    @Query(value = """
            SELECT id FROM configurator_options
            WHERE question_id = :questionId AND code = :code
            """, nativeQuery = true)
    Optional<Long> findAnyIdByQuestionIdAndCode(@Param("questionId") Long questionId,
            @Param("code") String code);

    /** {@code long} y no {@code boolean}: literal booleano proyectado, #196. */
    @Query(value = """
            SELECT COUNT(*) FROM configurator_options
            WHERE question_id = :questionId AND code = :code AND enabled = TRUE
            """, nativeQuery = true)
    long countEnabledByQuestionIdAndCode(@Param("questionId") Long questionId,
            @Param("code") String code);

    /**
     * Con {@code version = version + 1} en el {@code SET}: la tabla está versionada
     * ({@code UPDATE_MASIVO_MUEVE_LA_VERSION}, #53). La versión no va en el
     * {@code WHERE} — ver el javadoc gemelo en
     * {@code ConfiguratorQuestionJpaRepository}.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query(value = """
            UPDATE configurator_options
            SET enabled = TRUE, version = version + 1
            WHERE id = :id
            """, nativeQuery = true)
    int reactivate(@Param("id") Long id);
}
