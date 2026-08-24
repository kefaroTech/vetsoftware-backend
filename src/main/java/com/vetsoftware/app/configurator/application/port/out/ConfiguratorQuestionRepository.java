package com.vetsoftware.app.configurator.application.port.out;

import com.vetsoftware.app.configurator.application.dto.LinkStateDto;
import com.vetsoftware.app.configurator.domain.ConfiguratorQuestion;
import com.vetsoftware.app.shared.pagination.PageResult;
import java.util.List;
import java.util.Optional;

/**
 * Ningún método recibe {@code companyId} y no es un olvido:
 * {@code configurator_questions} no tiene esa columna. El cuestionario es de la
 * plataforma, así que lo que aísla no es un {@code WHERE} sino que sus
 * endpoints de edición sean {@code SYSTEM}.
 */
public interface ConfiguratorQuestionRepository {

    ConfiguratorQuestion save(ConfiguratorQuestion question);

    Optional<ConfiguratorQuestion> findById(Long id);

    /**
     * Todas las preguntas activas en el orden del cuestionario ({@code sort_order},
     * con desempate por {@code id}). Sin paginar a propósito: la comprobación de
     * ciclos y el render del cuestionario necesitan el árbol entero, que son
     * decenas de filas.
     */
    List<ConfiguratorQuestion> findAllOrdered();

    PageResult<ConfiguratorQuestion> findAll(int page, int pageSize);

    /**
     * Solo ve las preguntas activas. <strong>No sirve como guarda del alta</strong>
     * —{@code uq_configurator_questions_code} no incluye {@code enabled}, así que
     * una pregunta retirada sigue ocupando su código—; para eso está
     * {@link #findAnyByCode(String)}.
     *
     * <p>
     * Hoy <strong>no lo llama ningún caso de uso</strong>: sobrevive porque
     * {@code ConfiguratorQuestionPersistenceIT} lo usa para dejar escrito el hecho
     * que producía el defecto —la fila retirada sigue ahí y esta consulta no la
     * ve—. Si se borra, esa aserción se reescribe contra
     * {@link #findAnyByCode(String)}, que afirma lo mismo sobre el método que ahora
     * manda. Está registrado como incidencia aparte porque el nombre invita a
     * reintroducir el defecto.
     */
    boolean existsByCode(String code);

    /**
     * El código <strong>ignorando el borrado lógico</strong>, que es como lo mira
     * la clave única. Ver {@link LinkStateDto}.
     */
    Optional<LinkStateDto> findAnyByCode(String code);

    /** Deshace la baja lógica. Devuelve las filas afectadas (0 o 1). */
    int reactivate(Long id);

    /**
     * Si de la opción cuelga alguna pregunta condicional activa, esa opción no se
     * puede dar de baja: la rama que abría quedaría inalcanzable y sus efectos
     * dejarían de dispararse sin que nadie lo note.
     */
    boolean existsByParentOptionId(Long optionId);

    void delete(Long id);
}
