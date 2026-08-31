package com.vetsoftware.app.catalogitemaihint.domain;

/**
 * Ese articulo ya tiene una pista vigente.
 *
 * <p>
 * Es lo que separa «publicar la primera» de «corregir la que hay», y por eso
 * publicar no sucede en silencio a la anterior: un {@code POST} repetido por un
 * doble clic dejaria la revision 1 marcada como reemplazada y una revision 2
 * identica en el historial, sin que nadie lo pidiera. Quien quiere cambiar el
 * texto usa el camino que lo dice.
 *
 * <p>
 * Espejo en el motor de {@code uq_catalog_item_ai_hints_current}, a traves de
 * la columna generada {@code current_hint_marker}.
 */
public class CatalogItemAiHintAlreadyPublishedException extends RuntimeException {

    public CatalogItemAiHintAlreadyPublishedException(Long catalogItemId) {
        super("Catalog item " + catalogItemId + " already has a current AI hint:"
                + " publish a revision instead of a new hint");
    }
}
