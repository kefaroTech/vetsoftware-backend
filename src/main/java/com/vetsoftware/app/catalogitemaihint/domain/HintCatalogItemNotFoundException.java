package com.vetsoftware.app.catalogitemaihint.domain;

/**
 * El articulo al que se le quiere poner pista no existe, o esta desactivado.
 *
 * <p>
 * Sin esta guarda la peticion llegaria a la base y la clave foranea
 * {@code fk_catalog_item_ai_hints_item} la pararia con un
 * {@code DataIntegrityViolation}, que el cliente lee como un 500: un error del
 * servidor por un dato que escribio el cliente.
 */
public class HintCatalogItemNotFoundException extends RuntimeException {

    public HintCatalogItemNotFoundException(Long catalogItemId) {
        super("Catalog item " + catalogItemId + " not found");
    }
}
