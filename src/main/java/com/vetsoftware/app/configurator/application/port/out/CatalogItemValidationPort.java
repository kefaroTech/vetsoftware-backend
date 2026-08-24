package com.vetsoftware.app.configurator.application.port.out;

/**
 * La FK de {@code configurator_effects.catalog_item_id} a
 * {@code catalog_items}, que es de otra feature.
 *
 * <p>
 * Es un {@code ValidationPort} y no un {@code QueryPort} porque el configurador
 * <strong>no usa ningún campo del artículo</strong>: la resolución devuelve ids
 * y cantidades, y el nombre, el tipo y el precio los congela
 * {@code quote_lines} al cotizar, con la lista de precios de esa oferta. Traer
 * aquí un {@code CatalogItemRef} sería copiar datos que esta feature no lee y
 * atarla a la forma de otra.
 *
 * <p>
 * {@code catalog_items} no tiene {@code company_id} —es el catálogo global de
 * la plataforma— así que esta referencia no acerca el slice a
 * {@code CompanyJpaEntity} ni despierta las cuatro reglas de tenant.
 */
public interface CatalogItemValidationPort {

    /** {@code true} si el artículo existe y está activo. */
    boolean existsById(Long catalogItemId);
}
