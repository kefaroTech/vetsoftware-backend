package com.vetsoftware.app.configurator.application.port.out;

import com.vetsoftware.app.configurator.application.dto.LinkStateDto;
import com.vetsoftware.app.configurator.domain.ConfiguratorOption;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface ConfiguratorOptionRepository {

    ConfiguratorOption save(ConfiguratorOption option);

    Optional<ConfiguratorOption> findById(Long id);

    /**
     * Todas las opciones activas, ordenadas por pregunta y por {@code sortOrder}.
     */
    List<ConfiguratorOption> findAllOrdered();

    /** Las opciones activas de una pregunta, en su orden de presentación. */
    List<ConfiguratorOption> findByQuestionId(Long questionId);

    /**
     * Las opciones activas de VARIAS preguntas de una vez, agrupadas por pregunta y
     * en su orden de presentación.
     *
     * <p>
     * Es lo que convierte las {@code 1 + 1 + N} peticiones del editor del
     * cuestionario en dos consultas (incidencia #448): una página de cuarenta
     * preguntas se resolvía con cuarenta llamadas a
     * {@link #findByQuestionId(Long)}, y no al abrir la pantalla sino <em>en cada
     * guardado</em>.
     *
     * <p>
     * Ve exactamente lo mismo que {@link #findByQuestionId(Long)} —solo las
     * activas, por el {@code @SQLRestriction} de la entidad—, así que anidar no
     * cambia qué filas ve el editor: cambia cuántas veces las pide.
     */
    Map<Long, List<ConfiguratorOption>> findByQuestionIds(Collection<Long> questionIds);

    /**
     * Solo ve las opciones activas. <strong>No sirve como guarda del alta</strong>:
     * {@code uq_configurator_options_code} no incluye {@code enabled}. Para eso
     * está {@link #findAnyByQuestionIdAndCode(Long, String)}.
     *
     * <p>
     * Igual que su gemelo {@code ConfiguratorQuestionRepository.existsByCode}, hoy
     * no lo llama ningún caso de uso: sobrevive por las aserciones de
     * {@code ConfiguratorOptionPersistenceIT}.
     */
    boolean existsByQuestionIdAndCode(Long questionId, String code);

    /**
     * El par {@code (questionId, code)} <strong>ignorando el borrado
     * lógico</strong>, que es como lo mira la clave única. Ver
     * {@link LinkStateDto}.
     */
    Optional<LinkStateDto> findAnyByQuestionIdAndCode(Long questionId, String code);

    /** Deshace la baja lógica. Devuelve las filas afectadas (0 o 1). */
    int reactivate(Long id);

    /** Si la pregunta todavía tiene opciones activas, no se puede dar de baja. */
    boolean existsByQuestionId(Long questionId);

    void delete(Long id);
}
