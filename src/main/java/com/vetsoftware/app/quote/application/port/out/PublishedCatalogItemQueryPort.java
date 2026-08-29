package com.vetsoftware.app.quote.application.port.out;

import com.vetsoftware.app.quote.domain.BillingCycle;
import java.util.Collection;
import java.util.List;
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

    /**
     * Los rotulos de los componentes que cuelgan de los paquetes nombrados.
     *
     * <p>
     * <b>Existe para no cobrar dos veces.</b> Un paquete se cobra por su propio
     * precio y sus piezas <em>no</em> se cobran aparte: {@code CreateQuoteService}
     * pone precio a cada linea que recibe, una por una, y <b>no</b> expande ningun
     * paquete en sus componentes. Eso hace correctos los dos casos simples —
     * {@code SURGERY} suelto se cobra una vez, {@code PACK_CLINIC} se cobra una
     * vez— y deja abierto el tercero: una cesta con {@code PACK_CLINIC} <em>y</em>
     * {@code SCHEDULING} produce dos lineas y dos cobros por la misma
     * funcionalidad, porque {@code SCHEDULING} ya viene dentro del paquete.
     *
     * <p>
     * Hasta hoy eso no pasaba porque el front no mandaba lineas de modulo — una
     * convencion del llamador, no una proteccion. En cuanto el catalogo publica el
     * precio de cada modulo suelto, cualquiera puede componer esa cesta a mano, asi
     * que la comprobacion tiene que estar en el servidor. Este metodo es el dato
     * con el que {@code SelfServeQuoteService} la hace.
     *
     * <p>
     * <b>Aqui si se puede nombrar lo que se rechaza</b>, al contrario que en
     * {@link #findPublishedIdByCode}: los rotulos que devuelve son componentes de
     * paquetes publicados, y esos mismos rotulos con su composicion los publica ya
     * {@code GET /catalog} a cualquier anonimo. No hay oraculo que reabrir porque
     * no hay nada que averiguar.
     *
     * @param codes
     *            rotulos a examinar. Los que no sean paquetes simplemente no
     *            aportan filas; no hace falta saber de antemano cual es cual.
     * @return los rotulos de componente, sin repetir y en orden estable. Vacio si
     *         {@code codes} viene nulo o vacio.
     */
    List<String> findComponentCodesOfBundles(Collection<String> codes);

    /**
     * Los rotulos que la cesta <b>necesita</b> para funcionar y no trae.
     *
     * <p>
     * <b>Es la barandilla, no la amabilidad.</b> El configurador ya completa la
     * seleccion con sus requisitos, pero eso vive en el camino amable: quien llame
     * directo a este endpoint —o quien arme la cesta a mano desde el catalogo
     * publico— se lo salta entero. {@code catalog_item_dependencies} declara nueve
     * arcos {@code REQUIRES} desde el changeset 309 y hasta hoy no los evaluaba
     * nadie: facturar electronicamente sin Caja se cotizaba tal cual, y el cliente
     * compraba algo que no puede usar.
     *
     * <p>
     * <b>La cobertura expande los paquetes; el precio NO.</b> Es la unica sutileza
     * y es la que hace correcto el caso normal: {@code PACK_FULL} trae dentro
     * {@code ELECTRONIC_INVOICING} y {@code CASH_REGISTER}, asi que comprar el
     * paquete satisface el requisito aunque ninguno de los dos rotulos viaje en la
     * peticion. Sin esa expansion, la barandilla rechazaria la compra de un paquete
     * entero. Son dos expansiones distintas de la misma cesta para dos preguntas
     * distintas: la de arriba sirve para no cobrar dos veces, esta para no vender
     * algo inservible.
     *
     * <p>
     * <b>Solo se miran los requisitos de lo que se pidio</b>, no los de los
     * componentes de un paquete. Si un paquete trajera una pieza cuyo requisito el
     * propio paquete no cubre, eso es un defecto del catalogo y no del comprador:
     * rechazarle la compra le haria pagar un error ajeno que no puede arreglar.
     *
     * <p>
     * Aqui tambien se puede nombrar lo que falta, por lo mismo que en
     * {@link #findComponentCodesOfBundles}: son rotulos que el catalogo publico
     * ensena a cualquier anonimo, asi que no hay oraculo que reabrir — y un error
     * mudo obligaria al cliente a adivinar que le falta.
     */
    List<String> findMissingRequirements(Collection<String> codes);
}
