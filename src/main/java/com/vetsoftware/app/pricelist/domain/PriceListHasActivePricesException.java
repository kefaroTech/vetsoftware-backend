package com.vetsoftware.app.pricelist.domain;

/**
 * Se intentó dar de baja una lista que todavía tiene precios activos.
 *
 * <p>
 * El borrado es lógico, así que la fila de la lista desaparecería del
 * {@code @SQLRestriction} dejando vivos unos {@code catalog_prices} que ya no
 * cuelgan de nada visible: precios huérfanos que ninguna consulta explica y que
 * la FK {@code RESTRICT} no impide, porque nadie borra físicamente.
 */
public class PriceListHasActivePricesException extends RuntimeException {

    private final Long priceListId;
    private final long activePrices;

    public PriceListHasActivePricesException(Long priceListId, long activePrices) {
        super("Price list " + priceListId + " still has " + activePrices
                + " active prices: remove them first");
        this.priceListId = priceListId;
        this.activePrices = activePrices;
    }

    public Long getPriceListId() {
        return priceListId;
    }

    public long getActivePrices() {
        return activePrices;
    }
}
