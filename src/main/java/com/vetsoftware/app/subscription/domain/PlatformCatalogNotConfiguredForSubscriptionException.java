package com.vetsoftware.app.subscription.domain;

import java.util.Set;

/**
 * No hay catalogo comercial suficiente para firmar un contrato inicial, asi que
 * no se firma ninguno.
 *
 * <p>
 * Es deliberadamente bloqueante, y las dos alternativas son peores. Crear la
 * empresa sin contrato deja una cuenta que entra al sistema, no tiene
 * {@code company_entitlements} y no puede hacer nada sin ningun mensaje que lo
 * explique — se investiga como un problema de permisos y hay que borrarla a
 * mano de la base. Inventar un contrato vacio es peor todavia: mete una fila en
 * {@code subscriptions} que no corresponde a ningun articulo comprado, y
 * corrompe el dato del que cuelga toda la facturacion.
 *
 * <p>
 * Quien la traduce al mensaje que ve el usuario es el adaptador de
 * {@code registration}, que sabe de que empresa se trata y enumera las cinco
 * piezas que faltan.
 *
 * <p>
 * GlobalExceptionHandler: <strong>409</strong>,
 * {@code PLATFORM_CATALOG_NOT_CONFIGURED}.
 */
public class PlatformCatalogNotConfiguredForSubscriptionException extends RuntimeException {
    public PlatformCatalogNotConfiguredForSubscriptionException(Long companyId) {
        super("No platform catalog available to create the initial contract for company "
                + companyId);
    }

    /**
     * El catalogo resuelve el nucleo pero no las capacidades minimas.
     *
     * <p>
     * Es un mensaje distinto porque es un fallo distinto y se arregla en otro
     * sitio: alli falta el articulo {@code CORE} o su tarifa; aqui falta un
     * {@code catalog_items} de tipo {@code CAPACITY} con {@code is_core = TRUE}
     * para cada unidad que se nombra. Sin este mensaje, el sintoma llegaba tres
     * pasos mas adelante como un {@code 404 COMPANY_CAPACITY_NOT_FOUND} contra una
     * empresa que ya no existia por el rollback, y no señalaba al catalogo (#490).
     */
    public PlatformCatalogNotConfiguredForSubscriptionException(Long companyId,
            Set<CapacityUnit> missingUnits) {
        super("The platform catalog grants no core capacity for " + missingUnits
                + ", so the initial contract of company " + companyId
                + " would not let it create its own first branch or user: seed one ACTIVE"
                + " catalog_items with item_type = 'CAPACITY', is_core = TRUE and the matching"
                + " capacity_unit, plus its published catalog_prices tier");
    }
}
