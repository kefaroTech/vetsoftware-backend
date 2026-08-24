package com.vetsoftware.app.pricelist.application.port.out;

/**
 * Comprueba que el articulo al que se le pone precio existe.
 *
 * <p>
 * Es un {@code ValidationPort} y no un {@code QueryPort} porque esta feature
 * <strong>no necesita ningun dato</strong> del articulo: el precio se
 * identifica por {@code (lista, articulo, ciclo, tramo)} y el codigo y el
 * nombre comerciales viven en el slice {@code catalogitem}, que es quien los
 * sirve. Es el caso que {@code CLAUDE.md} describe como "no necesitas datos del
 * agregado externo, solo el ID".
 *
 * <p>
 * Devuelve un booleano en vez de lanzar: la excepcion de FK inexistente la
 * decide el caso de uso, nunca el adaptador.
 *
 * <p>
 * {@code REFERENCIAS_CROSS_FEATURE_ACOTADAS_POR_EMPRESA} no aplica:
 * {@code catalog_items} no tiene {@code company_id}, asi que no hay variante
 * acotada que ofrecer.
 */
public interface CatalogItemValidationPort {

    boolean existsById(Long catalogItemId);
}
