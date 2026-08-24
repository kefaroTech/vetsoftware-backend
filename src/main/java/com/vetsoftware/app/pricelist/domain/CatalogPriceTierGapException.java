package com.vetsoftware.app.pricelist.domain;

/**
 * Los tramos de un {@code (catalog_item_id, billing_cycle)} de la lista dejan
 * una cantidad sin cubrir.
 *
 * <p>
 * Es la otra mitad de la ficha 6 de {@code suscripciones-tablas.md}, que pide
 * que los tramos de un mismo articulo <em>no se pisen y no dejen huecos</em>.
 * {@link CatalogPrice#requireNoTierOverlap} cubria la primera; esta cubre la
 * segunda (incidencia #378).
 *
 * <p>
 * <strong>Por que al publicar y no al insertar.</strong> La continuidad es una
 * propiedad de la lista completa, no de cada fila: exigirla en cada
 * {@code POST} haria imposible construir una tarifa incrementalmente, porque el
 * primer tramo {@code [11,20]} de una lista vacia ya seria un hueco del 1 al
 * 10. Publicar es el unico momento en que la lista esta completa, y es tambien
 * el ultimo en que se puede corregir: despues manda R9 y hay que emitir otra.
 *
 * <p>
 * El hueco viaja como dato ademas de dentro del mensaje. Sin el, la consola
 * solo puede decir «hay un hueco» y quien monta la tarifa tiene que buscarlo a
 * mano entre los tramos; con el, puede senalar el artículo y el rango exacto
 * que falta.
 */
public class CatalogPriceTierGapException extends RuntimeException {

    private final Long priceListId;
    private final Long catalogItemId;
    private final BillingCycle billingCycle;
    private final int gapFrom;
    private final Integer gapTo;

    /**
     * @param gapTo
     *            {@code null} significa «del {@code gapFrom} en adelante»: el
     *            ultimo tramo declarado es cerrado y nadie cubre lo que hay por
     *            encima.
     */
    public CatalogPriceTierGapException(Long priceListId, Long catalogItemId,
            BillingCycle billingCycle, int gapFrom, Integer gapTo) {
        super("Price list " + priceListId + " cannot be published: catalog item " + catalogItemId
                + " on cycle " + billingCycle + " has no price for quantities " + gapFrom
                + (gapTo == null ? " and above" : " to " + gapTo));
        this.priceListId = priceListId;
        this.catalogItemId = catalogItemId;
        this.billingCycle = billingCycle;
        this.gapFrom = gapFrom;
        this.gapTo = gapTo;
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

    public int getGapFrom() {
        return gapFrom;
    }

    public Integer getGapTo() {
        return gapTo;
    }
}
