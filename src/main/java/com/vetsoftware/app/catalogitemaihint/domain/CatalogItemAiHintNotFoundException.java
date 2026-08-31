package com.vetsoftware.app.catalogitemaihint.domain;

/**
 * El articulo no tiene pista vigente.
 *
 * <p>
 * Es el 404 de corregir y de retirar, y tambien el de leer la vigente. No
 * significa que el articulo no exista —eso es
 * {@link HintCatalogItemNotFoundException}— sino que nadie le ha publicado una
 * pista todavia, o que la ultima se retiro. Distinguirlos importa: sobre el
 * primero no hay nada que hacer, sobre el segundo el camino es publicar.
 */
public class CatalogItemAiHintNotFoundException extends RuntimeException {

    public CatalogItemAiHintNotFoundException(Long catalogItemId) {
        super("Catalog item " + catalogItemId + " has no current AI hint");
    }
}
