package com.vetsoftware.app.pricelist.application.port.out;

import com.vetsoftware.app.pricelist.application.dto.PublicCatalogItemRowDto;
import com.vetsoftware.app.pricelist.application.dto.PublicCatalogPackComponentRowDto;
import com.vetsoftware.app.pricelist.application.dto.PublicPlanRowDto;
import java.util.List;

/**
 * El read model del catalogo <strong>contratable</strong>: todo lo que se puede
 * comprar suelto, con su precio en los dos ciclos.
 *
 * <p>
 * <strong>Por que un puerto nuevo y no tres metodos mas en
 * {@link PublicPlanQueryPort}.</strong> Aquel responde «que paquetes vendo y
 * que traen dentro» y su contrato esta cerrado: sus tres consultas y su rodaja
 * ({@code PublicPlanQueryPortIT}) describen el catalogo de paquetes y nada mas.
 * Este responde «que puede comprar alguien que no quiere un paquete». Meterlos
 * en la misma interfaz haria que cualquier columna nueva del catalogo suelto
 * apareciera al alcance del servicio de planes, que es exactamente la fuga
 * silenciosa que la separacion de puertos publicos existe para evitar — el
 * mismo razonamiento por el que {@code GetPublicPlansUseCase} vive separado de
 * los puertos de administracion.
 *
 * <p>
 * <strong>Solo lee, y solo catalogo global.</strong> {@code catalog_items},
 * {@code catalog_prices} y {@code bundle_components} no tienen
 * {@code company_id}, asi que ni {@code LISTADOS_SIN_EMPRESA_SOLO_SYSTEM} ni
 * {@code REFERENCIAS_CROSS_FEATURE_ACOTADAS_POR_EMPRESA} aplican: no hay
 * empresa que acotar porque no hay empresa en el dato.
 *
 * <p>
 * La tarifa vigente no se elige aqui. La sigue eligiendo el caso de uso con
 * {@link PublicPlanQueryPort#findPublishedPriceLists()} y
 * {@code PublicPriceListSelector}, que es lo que garantiza que
 * {@code GET /plans} y {@code GET /catalog} publiquen precios de la
 * <em>misma</em> lista el mismo dia.
 */
public interface PublicCatalogQueryPort {

    /**
     * Los articulos sueltos —{@code MODULE}, {@code CAPACITY} y {@code ONE_TIME}—
     * que estan {@code ACTIVE} y tienen precio de entrada en al menos uno de los
     * dos ciclos de esa tarifa.
     *
     * <p>
     * Los {@code BUNDLE} quedan fuera: los sirve {@link #findPacks(Long)}, con su
     * composicion. Un articulo sin ningun precio en la tarifa vigente queda fuera
     * tambien — no se puede comprar, asi que anunciarlo es prometer lo que no hay.
     */
    List<PublicCatalogItemRowDto> findContractableItems(Long priceListId);

    /**
     * Los paquetes con precio de entrada en esa tarifa.
     *
     * <p>
     * Devuelve {@link PublicPlanRowDto} —el mismo record que
     * {@link PublicPlanQueryPort#findPlans(Long)}— y no uno nuevo: la cabecera de
     * un paquete es exactamente la misma pregunta con la misma respuesta, y
     * duplicar el record duplicaria tambien el esquema en el contrato OpenAPI sin
     * anadir un solo campo.
     */
    List<PublicPlanRowDto> findPacks(Long priceListId);

    /**
     * El grafo paquete → componente, por rotulos. Es el mismo grafo contra el que
     * {@code SelfServeQuoteService} rechaza una cesta que mezcle un paquete con una
     * pieza suya.
     */
    List<PublicCatalogPackComponentRowDto> findPackComponents(Long priceListId);
}
