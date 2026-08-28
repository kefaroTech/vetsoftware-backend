package com.vetsoftware.app.quote.application.port.out;

import com.vetsoftware.app.quote.domain.BillingCycle;
import com.vetsoftware.app.quote.domain.CatalogPriceRef;
import java.util.List;

/**
 * Lee los tramos de precio del articulo en la tarifa cotizada, UNA SOLA VEZ,
 * para congelarlos en las lineas.
 *
 * <p>
 * El precio nunca llega del cliente: si el importe fuera un campo del
 * formulario, cotizar a cero seria trivial.
 *
 * <p>
 * <b>Devuelve el CONJUNTO de tramos y no "el tramo aplicable".</b> La version
 * anterior devolvia uno solo -el de {@code tier_min} mas alto que no superara
 * la cantidad- y multiplicaba todo por el: quince usuarios salian a 117.000 en
 * vez de a los 141.000 que decidio D-66. La aritmetica acumulativa no se puede
 * construir sobre una consulta que ya descarto los tramos bajos, asi que el
 * recorte se retiro del SQL y el reparto vive en el dominio
 * ({@link com.vetsoftware.app.quote.domain.TieredPrice}), donde se comprueba en
 * cada lectura.
 */
public interface CatalogPriceQueryPort {

    /**
     * TODOS los tramos activos del articulo en esa tarifa y ciclo, ordenados por
     * {@code tier_min} ascendente. Lista vacia si el articulo no esta tarifado ahi.
     */
    List<CatalogPriceRef> findAllTiers(Long priceListId, Long catalogItemId,
            BillingCycle billingCycle);
}
