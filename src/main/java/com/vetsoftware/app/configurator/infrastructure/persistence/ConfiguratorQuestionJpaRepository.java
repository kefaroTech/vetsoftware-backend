package com.vetsoftware.app.configurator.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface ConfiguratorQuestionJpaRepository
        extends
            JpaRepository<ConfiguratorQuestionJpaEntity, Long> {

    /**
     * El orden del cuestionario. Desempata por {@code id} porque {@code sort_order}
     * no es único: sin el desempate, dos lecturas del mismo cuestionario pueden
     * devolver dos órdenes distintos y el asistente pinta las preguntas movidas.
     */
    List<ConfiguratorQuestionJpaEntity> findAllByOrderBySortOrderAscIdAsc();

    /**
     * El {@code @SQLRestriction} de la entidad ya acota esto a las filas activas,
     * que es justo lo que <strong>no</strong> sirve para guardar el alta: ver
     * {@link #findAnyIdByCode(String)}.
     */
    boolean existsByCode(String code);

    boolean existsByParentOptionId(Long parentOptionId);

    /**
     * Ignora el borrado lógico: {@code uq_configurator_questions_code} tampoco lo
     * ignora, así que una pregunta dada de baja sigue ocupando su código aunque la
     * aplicación no la vea. Nativa porque es la única forma de esquivar el
     * {@code @SQLRestriction}.
     */
    @Query(value = "SELECT id FROM configurator_questions WHERE code = :code", nativeQuery = true)
    Optional<Long> findAnyIdByCode(@Param("code") String code);

    /**
     * Devuelve {@code long} y no {@code boolean} a propósito: proyectar un literal
     * booleano en una {@code @Query} es exactamente lo que prohíbe
     * {@code PROYECCION_SIN_LITERAL_BOOLEANO} (incidencia #196).
     */
    @Query(value = """
            SELECT COUNT(*) FROM configurator_questions
            WHERE code = :code AND enabled = TRUE
            """, nativeQuery = true)
    long countEnabledByCode(@Param("code") String code);

    /**
     * Sube {@code version} porque este {@code UPDATE} va directo a la base: no pasa
     * por el ciclo leer-modificar-guardar, así que {@code @Version} no lo comprueba
     * ni lo incrementa. Sin moverla, un {@code save} concurrente cargado antes
     * reescribe la fila entera desde el dominio —con su {@code enabled = false}— y
     * su {@code WHERE version = ?} casa igual, así que deshace la reactivación en
     * silencio ({@code UPDATE_MASIVO_MUEVE_LA_VERSION}, incidencia #53). Esta tabla
     * <strong>sí</strong> está versionada, al contrario que las tres puentes de
     * {@code catalogitem} de las que se copia el patrón.
     *
     * <p>
     * Y la versión <strong>no</strong> va en el {@code WHERE}: reactivar es
     * deliberado y debe ejecutarse siempre, no competir con una edición. Ahí solo
     * conseguiría actualizar cero filas y que el servicio lo leyera como «no
     * existe».
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query(value = """
            UPDATE configurator_questions
            SET enabled = TRUE, version = version + 1
            WHERE id = :id
            """, nativeQuery = true)
    int reactivate(@Param("id") Long id);
}
