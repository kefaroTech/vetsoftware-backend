package com.vetsoftware.app.catalogitemaihint.infrastructure.persistence;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/**
 * <strong>Ni un solo {@code @Query} de {@code UPDATE}.</strong> Marcar una
 * pista como reemplazada podria escribirse como
 * {@code UPDATE … SET superseded_at = :ahora, version = version + 1 WHERE id = :id}
 * —y {@code UPDATE_MASIVO_MUEVE_LA_VERSION} obligaria a mover la version en el
 * {@code SET}, porque esa sentencia va directa a la base sin pasar por el ciclo
 * leer-modificar-guardar—. Aqui se hace por el camino del agregado
 * ({@code supersede()} + {@code saveAndFlush}), que ademas de no necesitar esa
 * regla obtiene el chequeo optimista de regalo: Hibernate anade
 * {@code AND version = ?} al UPDATE, asi que dos correcciones simultaneas del
 * mismo articulo no se pisan en silencio —la segunda falla—. Un
 * {@code @Modifying} sin {@code WHERE version = ?} las dejaria pasar a las dos.
 *
 * <p>
 * {@code REPOS_CON_ENTITYGRAPH} no aplica: {@code CatalogItemAiHintJpaEntity}
 * no declara ninguna asociacion {@code @ManyToOne}, asi que no hay proxy LAZY
 * que hidratar ni N+1 que evitar.
 */
public interface CatalogItemAiHintJpaRepository
        extends
            JpaRepository<CatalogItemAiHintJpaEntity, Long> {

    /**
     * La vigente. {@code superseded_at IS NULL} es la misma condicion que sostiene
     * {@code uq_catalog_item_ai_hints_current} a traves de la columna generada
     * {@code current_hint_marker}, asi que el indice garantiza que hay como mucho
     * una.
     */
    Optional<CatalogItemAiHintJpaEntity> findByCatalogItemIdAndSupersededAtIsNull(
            Long catalogItemId);

    /** Las vigentes de todos los articulos. */
    Page<CatalogItemAiHintJpaEntity> findBySupersededAtIsNull(Pageable pageable);

    /** El historial de un articulo, vigentes y reemplazadas. */
    Page<CatalogItemAiHintJpaEntity> findByCatalogItemId(Long catalogItemId, Pageable pageable);

    /**
     * Pregunta por {@code uq_catalog_item_ai_hints_text} usando su misma clave: la
     * columna generada {@code hint_hash}, no el texto en claro. Comparar
     * {@code hint_text} directamente lo haria bajo la colacion de la columna
     * —insensible a mayusculas—, mientras que el indice compara sobre
     * {@code ascii_bin}: la guarda rechazaria una correccion legitima que solo
     * cambia una mayuscula, y seguiria sin ser la restriccion que dice cubrir.
     */
    boolean existsByCatalogItemIdAndHintHash(Long catalogItemId, String hintHash);

    /**
     * El ultimo numero de revision publicado para ese articulo. Es un
     * {@code SELECT} agregado, no una mutacion, y mira el historial entero: por eso
     * republicar despues de retirar continua la numeracion en vez de chocar contra
     * {@code uq_catalog_item_ai_hints_revision}.
     */
    @Query("select max(h.hintRevision) from CatalogItemAiHintJpaEntity h "
            + "where h.catalogItemId = :catalogItemId")
    Optional<Integer> findLastRevision(Long catalogItemId);
}
