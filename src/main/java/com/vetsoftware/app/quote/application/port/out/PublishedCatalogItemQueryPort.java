package com.vetsoftware.app.quote.application.port.out;

import com.vetsoftware.app.quote.domain.BillingCycle;
import java.util.Optional;

/**
 * Traduce el <b>rotulo publico</b> de un articulo a su id, y solo si ese
 * articulo es de los que la portada ya publica.
 *
 * <p>
 * <b>Por que existe.</b> {@code GET /plans} nombra los articulos por
 * {@code code} y no publica ningun id, a proposito. Sin este traductor, la
 * autocontratacion pedia {@code catalogItemId} y no habia ninguna ruta por la
 * que un empleado del tenant lo obtuviera —{@code GET /catalog-items} es
 * {@code hasRole('SYSTEM')}—: el endpoint existia con permiso sembrado y cero
 * llamadores posibles.
 *
 * <p>
 * <b>Y por que NO es simplemente «buscar por code».</b> Un traductor que
 * resolviera cualquier {@code code} del catalogo seria la puerta de atras que
 * {@code GET /catalog-items} cierra por delante: bastaria probar rotulos para
 * enumerar articulos internos, en borrador o retirados. Este resuelve
 * <b>exclusivamente el mismo conjunto que devuelve {@code GET /plans}</b> para
 * la tarifa y el ciclo con los que se esta cotizando, y devuelve
 * {@link Optional#empty()} para todo lo demas <b>sin distinguir el motivo</b>:
 * un codigo inexistente y un codigo interno son indistinguibles desde fuera. Si
 * alguna vez alguien afloja este predicado —«total, es solo una lectura»— habra
 * reabierto el oraculo, no relajado una validacion.
 *
 * <p>
 * Catalogo global de plataforma: {@code catalog_items},
 * {@code bundle_components} y {@code catalog_prices} no tienen
 * {@code company_id}, asi que no hay empresa que acotar. Solo lee.
 */
public interface PublishedCatalogItemQueryPort {

    /**
     * El id del articulo con ese {@code code}, <b>solo</b> si hoy es contratable
     * por autoservicio: paquete {@code ACTIVE} publicado, o modulo/capacidad
     * {@code ACTIVE} que cuelga de un paquete publicado, y en los dos casos con
     * precio de entrada en esa tarifa y ese ciclo.
     *
     * <p>
     * {@link Optional#empty()} cubre todos los rechazos —no existe, esta en
     * borrador, esta retirado, es un cargo unico que la portada no anuncia, o no
     * esta tarifado en el ciclo pedido— y quien llama <b>no debe</b> intentar
     * averiguar cual: la indistinguibilidad es el punto.
     */
    Optional<Long> findPublishedIdByCode(String code, Long priceListId, BillingCycle billingCycle);
}
