package com.vetsoftware.app.submodule.application.port.out;

/**
 * ¿Hay alguna empresa que tenga concedido <strong>hoy</strong> este submodulo?
 *
 * <p>
 * Es la guarda simetrica de {@link CatalogItemChildrenQueryPort} (#413). Aquel
 * mira el lado del <b>catalogo comercial</b> --que se vende--; este mira el
 * lado del <b>inquilino</b> --quien lo tiene concedido--. Hacian falta los dos,
 * porque una concesion {@code MANUAL_GRANT} no tiene, por definicion, ninguna
 * fila en {@code catalog_item_sub_modules}: soporte se la da a una clinica sin
 * articulo de catalogo detras. Con una sola guarda, el borrado de ese submodulo
 * pasaba, la fila de {@code company_entitlements} se quedaba vigente y con
 * nivel {@code FULL}, y en la peticion siguiente los empleados perdian esos
 * codigos --la consulta de permisos efectivos exige
 * {@code sub_modules.enabled = TRUE}--. La ficha decia \"concedido\" y el
 * endpoint respondia 403.
 */
public interface CompanyEntitlementChildrenQueryPort {
    boolean existsActiveBySubModuleId(Long subModuleId);
}
