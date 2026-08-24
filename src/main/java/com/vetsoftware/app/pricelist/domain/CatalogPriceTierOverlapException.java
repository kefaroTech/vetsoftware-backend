package com.vetsoftware.app.pricelist.domain;

/**
 * Dos tramos del mismo {@code (price_list_id, catalog_item_id, billing_cycle)}
 * se pisan.
 *
 * <p>
 * MySQL no tiene restricciones de exclusión —lo más cercano es el patrón de
 * columna generada, que solo cubre igualdad exacta—, así que
 * {@code uq_catalog_prices_tier} solo impide repetir el mismo {@code tier_min}
 * y deja pasar «del 1 al 10» junto a «del 5 al 20». Con dos tramos solapados,
 * qué precio se aplica a la unidad 7 depende del orden de recuperación, que no
 * es un contrato: el mismo cliente puede recibir dos cotizaciones distintas por
 * lo mismo.
 */
public class CatalogPriceTierOverlapException extends RuntimeException {

    private final Long priceListId;
    private final Long catalogItemId;
    private final BillingCycle billingCycle;
    private final Long conflictingPriceId;

    public CatalogPriceTierOverlapException(Long priceListId, Long catalogItemId,
            BillingCycle billingCycle, int tierMin, Integer tierMax, Long conflictingPriceId) {
        super("Tier [" + tierMin + ", " + (tierMax == null ? "*" : tierMax)
                + "] overlaps catalog price " + conflictingPriceId + " for price list "
                + priceListId + ", catalog item " + catalogItemId + " and cycle " + billingCycle);
        this.priceListId = priceListId;
        this.catalogItemId = catalogItemId;
        this.billingCycle = billingCycle;
        this.conflictingPriceId = conflictingPriceId;
    }

    public Long getPriceListId() {
        return priceListId;
    }

    public Long getCatalogItemId() {
        return catalogItemId;
    }

    public BillingCycle getBillingCycle() {
        return billingCycle;
    }

    public Long getConflictingPriceId() {
        return conflictingPriceId;
    }
}
