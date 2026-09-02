package com.vetsoftware.app.registration.domain;

/**
 * La plataforma no tiene catalogo comercial configurado, asi que no se puede
 * crear el contrato inicial de una empresa y el alta se cancela entera.
 *
 * <p>
 * Es deliberadamente bloqueante. La alternativa —crear la empresa y dejarle el
 * contrato «para despues»— produce una cuenta que entra al sistema, no tiene
 * {@code company_entitlements} y no puede hacer nada, sin ningun mensaje que lo
 * explique; hay que borrarla a mano de la base. El mensaje dice que falta y
 * como arreglarlo.
 */
public class PlatformCatalogNotConfiguredException extends RuntimeException {

    private static final String QUE_FALTA = """
            La plataforma no tiene catalogo comercial configurado, asi que no se puede crear el \
            contrato inicial de la empresa '%s' y el alta se cancela entera (no queda ninguna \
            empresa a medias). Falta el minimo estructural, en este orden: \
            (1) un catalog_items con code='CORE', item_type='MODULE', structural_minimum=true, status='ACTIVE'; \
            (2) al menos un catalog_item_sub_modules que ate ese articulo a un sub_modules con \
            is_sellable=true; \
            (3) una price_lists en status='PUBLISHED' con published_at y published_by_system_user_id; \
            (4) un catalog_prices de esa lista para el articulo CORE y el ciclo contratado; \
            (5) la fila unica de platform_billing_config con default_price_list_id apuntando a esa \
            lista; \
            (6) un catalog_items por cada unidad del minimo operable -BRANCH y USER-, con \
            item_type='CAPACITY', structural_minimum=true, su capacity_unit y status='ACTIVE'; \
            (7) un catalog_prices de la misma lista y ciclo para cada uno de esos articulos. \
            Las dos ultimas son las que permiten que la empresa cree su propia sede principal y \
            su propio dueño: sin ellas el alta muere despues, al reservar capacidad, con un error \
            que señala al recalculo de permisos y no al catalogo. \
            Siembra el catalogo (ver docs/db/suscripciones-datos-semilla.md) y reintenta el \
            alta.""";

    public PlatformCatalogNotConfiguredException(String companyName) {
        super(QUE_FALTA.formatted(companyName));
    }
}
