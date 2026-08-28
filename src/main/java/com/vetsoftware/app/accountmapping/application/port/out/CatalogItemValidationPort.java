package com.vetsoftware.app.accountmapping.application.port.out;

/**
 * La clave foranea {@code account_mappings.catalog_item_id} contra
 * {@code catalog_items(id)}, que es de otra feature.
 *
 * <p>
 * {@code ValidationPort} y no {@code QueryPort}: de aqui no se lee ni el nombre
 * ni el codigo del articulo. El mapeo solo necesita saber que existe, porque la
 * clave foranea es {@code RESTRICT} y un id inventado saldria como error de
 * integridad en vez de como «ese articulo no existe».
 *
 * <p>
 * <strong>Sin variante acotada por empresa:</strong> {@code catalog_items} es
 * el catalogo comercial de plataforma y no lleva {@code company_id}.
 */
public interface CatalogItemValidationPort {

    boolean existsById(Long catalogItemId);
}
