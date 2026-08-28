package com.vetsoftware.app.pricelist.domain;

/**
 * Un articulo ACTIVO del catalogo no tiene ni un precio en la lista que se
 * quiere publicar.
 *
 * <p>
 * <b>Es el agujero que dejaba la comprobacion de continuidad</b> (defecto
 * construido #16). {@link PriceListTierCoverage} agrupaba sobre los precios
 * ESCRITOS: un articulo sin ninguna fila no producia grupo, no producia hueco y
 * la lista se publicaba limpia. La continuidad no puede detectar una ausencia
 * total porque solo sabe mirar lo que existe; hace falta contrastar contra el
 * conjunto de articulos activos, que es lo que dice R-PRICE-05.
 *
 * <p>
 * <b>Y no es una molestia teorica.</b> Si el articulo olvidado es el nucleo,
 * ninguna empresa puede registrarse: el alta resuelve su contrato inicial
 * contra la tarifa vigente y se queda sin precio del nucleo. La tarifa se
 * publico sin un solo error, el fallo aparece en el primer registro que alguien
 * intente, y entre las dos cosas no hay ninguna senal que las relacione.
 *
 * <p>
 * El articulo viaja como dato ademas de dentro del mensaje: quien monta la
 * tarifa necesita saber CUAL falta, no que falta alguno.
 */
public class CatalogPriceMissingForActiveItemException extends RuntimeException {

    private final Long priceListId;
    private final Long catalogItemId;

    public CatalogPriceMissingForActiveItemException(Long priceListId, Long catalogItemId) {
        super("Price list " + priceListId + " cannot be published: active catalog item "
                + catalogItemId + " has no price in it");
        this.priceListId = priceListId;
        this.catalogItemId = catalogItemId;
    }

    public Long getPriceListId() {
        return priceListId;
    }

    public Long getCatalogItemId() {
        return catalogItemId;
    }
}
