package com.vetsoftware.app.configurator.application.port.out;

import com.vetsoftware.app.configurator.application.dto.LinkStateDto;
import com.vetsoftware.app.configurator.domain.ConfiguratorEffect;
import com.vetsoftware.app.configurator.domain.EffectType;
import com.vetsoftware.app.shared.pagination.PageResult;
import java.util.List;
import java.util.Optional;

public interface ConfiguratorEffectRepository {

    ConfiguratorEffect save(ConfiguratorEffect effect);

    Optional<ConfiguratorEffect> findById(Long id);

    /**
     * Todos los efectos activos, ordenados por {@code id} ascendente — que es el
     * orden en que los aplica {@code ConfiguratorResolver} y por tanto parte del
     * contrato de la resolución, no una preferencia del adaptador.
     */
    List<ConfiguratorEffect> findAllOrdered();

    PageResult<ConfiguratorEffect> findAll(int page, int pageSize);

    /** Si de la opción cuelgan efectos activos, no se puede dar de baja. */
    boolean existsByOptionId(Long optionId);

    /** Ídem para los efectos disparados por una pregunta numérica. */
    boolean existsByQuestionId(Long questionId);

    /**
     * Si de la pregunta cuelga algún efecto <em>activo</em>
     * {@code QUANTITY_FROM_ANSWER}. Es el otro lado de la invariante que vigila
     * {@code QuantityFromAnswerGuard} al crear un efecto: cambiar el
     * {@code answerType} de la pregunta a algo que no sea {@code NUMBER} dejaría
     * ese efecto vivo pero sin número que leer, y sin excepción, sin log y sin
     * línea de cero en la cotización.
     */
    boolean existsQuantityFromAnswerByQuestionId(Long questionId);

    /**
     * La terna de una de las dos claves únicas del efecto
     * —{@code (option_id, catalog_item_id, effect)} o su gemela por
     * {@code question_id}— <strong>ignorando el borrado lógico</strong>.
     * Exactamente uno de {@code optionId} y {@code questionId} viene relleno, como
     * impone la entidad. Ver {@link LinkStateDto}.
     */
    Optional<LinkStateDto> findAnyByTrigger(Long optionId, Long questionId, Long catalogItemId,
            EffectType effect);

    /** Deshace la baja lógica. Devuelve las filas afectadas (0 o 1). */
    int reactivate(Long id);

    void delete(Long id);
}
