package com.vetsoftware.app.catalogitemaihint.domain;

/**
 * Ese mismo texto ya se publico alguna vez bajo ese articulo.
 *
 * <p>
 * Espejo de {@code uq_catalog_item_ai_hints_text}, el indice unico sobre
 * {@code (catalog_item_id, hint_hash)}. La restriccion existe para que el
 * historico no se llene de revisiones identicas: si dos revisiones dicen
 * exactamente lo mismo, «con que texto se genero esta propuesta» deja de tener
 * una respuesta util.
 *
 * <p>
 * Se comprueba antes de escribir —no se deja saltar al motor— porque el error
 * de integridad sale como 500 y el cliente no puede distinguirlo de una caida.
 */
public class CatalogItemAiHintTextAlreadyPublishedException extends RuntimeException {

    public CatalogItemAiHintTextAlreadyPublishedException(Long catalogItemId) {
        super("That exact hint text was already published for catalog item " + catalogItemId);
    }
}
