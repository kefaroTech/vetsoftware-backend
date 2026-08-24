package com.vetsoftware.app.subscription.application.port.out;

/**
 * Valida que el articulo del catalogo existe. No trae sus datos a proposito: el
 * codigo, el nombre, el tipo y la unidad se <strong>congelan</strong> en la
 * linea al firmar, y leerlos otra vez del catalogo seria justamente la fuga que
 * el modelo evita —el catalogo cambia, el contrato firmado no—.
 *
 * <p>
 * {@code catalog_items} es catalogo global de plataforma y no lleva
 * {@code company_id}, asi que
 * {@code REFERENCIAS_CROSS_FEATURE_ACOTADAS_POR_EMPRESA} no aplica: no hay
 * empresa por la que acotar.
 */
public interface CatalogItemValidationPort {
    void validateExists(Long catalogItemId);
}
