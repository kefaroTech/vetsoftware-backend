package com.vetsoftware.app.catalogitemaihint.application.port.out;

import com.vetsoftware.app.catalogitemaihint.domain.CatalogItemAiHint;
import com.vetsoftware.app.shared.pagination.PageResult;
import java.util.Optional;

/**
 * No declara ningun {@code update}: lo unico que este puerto sabe hacer sobre
 * una fila viva es {@link #supersede}, que mueve {@code superseded_at} y nada
 * mas. El texto de una revision publicada no se toca.
 */
public interface CatalogItemAiHintRepository {

    CatalogItemAiHint save(CatalogItemAiHint hint);

    /**
     * Cierra la vigencia de una revision <strong>y la escribe en la base antes de
     * devolver</strong>.
     *
     * <p>
     * Existe aparte de {@link #save} por una razon medida, no por estilo. El indice
     * {@code uq_catalog_item_ai_hints_current} —sostenido por la columna generada
     * {@code current_hint_marker}— solo admite una revision vigente por articulo, y
     * al corregir hay un instante con dos: la que se sucede y la que entra.
     * Hibernate ordena el vaciado con <b>los INSERT antes que los UPDATE</b>, asi
     * que dejar las dos escrituras al flush de la transaccion emite primero el
     * INSERT de la nueva —cuando la vieja todavia tiene {@code superseded_at} nulo—
     * y MySQL rechaza la correccion entera con un «Duplicate entry … for key
     * uq_catalog_item_ai_hints_current». Es el mismo defecto que destapo la rodaja
     * de {@code legaldocumentversion}, y ningun test con este puerto mockeado puede
     * verlo, porque el orden del vaciado no existe fuera de la base.
     */
    CatalogItemAiHint supersede(CatalogItemAiHint hint);

    Optional<CatalogItemAiHint> findById(Long id);

    /**
     * La vigente de un articulo. {@code superseded_at IS NULL} es la misma
     * condicion que sostiene {@code uq_catalog_item_ai_hints_current}, asi que el
     * indice garantiza que hay como mucho una.
     */
    Optional<CatalogItemAiHint> findCurrentByCatalogItemId(Long catalogItemId);

    /**
     * El ultimo numero de revision publicado para ese articulo, <b>vigente o
     * no</b>. Es lo que hace que republicar despues de retirar continue la
     * numeracion en vez de chocar contra {@code uq_catalog_item_ai_hints_revision}.
     */
    Optional<Integer> findLastRevision(Long catalogItemId);

    /**
     * {@code true} si ese texto exacto ya se publico bajo ese articulo, vigente o
     * no. Pregunta por {@code uq_catalog_item_ai_hints_text} <em>a traves de la
     * huella</em>, que es el criterio que usa el indice: comparar el texto en claro
     * seria mas laxo o mas estricto que el motor segun la colacion de la columna, y
     * una guarda que no coincide con la restriccion que dice cubrir no cubre nada.
     */
    boolean existsPublishedText(Long catalogItemId, String hintText);

    /**
     * Las vigentes de todos los articulos, paginadas.
     *
     * <p>
     * <b>Sin variante acotada por empresa, y no falta ninguna</b>:
     * {@code catalog_item_ai_hints} no tiene {@code company_id} ni alcanza
     * {@code companies} por ninguna asociacion. Por eso el puerto de entrada que lo
     * sirve va cerrado a rol de sistema a secas, que es lo que exige
     * {@code LISTADOS_SIN_EMPRESA_SOLO_SYSTEM}.
     */
    PageResult<CatalogItemAiHint> findAllCurrent(int page, int pageSize);

    /** El historial de un articulo, de la revision mas nueva a la mas vieja. */
    PageResult<CatalogItemAiHint> findAllByCatalogItemId(Long catalogItemId, int page,
            int pageSize);
}
