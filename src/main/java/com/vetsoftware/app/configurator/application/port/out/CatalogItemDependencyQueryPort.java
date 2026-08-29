package com.vetsoftware.app.configurator.application.port.out;

import java.util.Map;
import java.util.Set;

/**
 * Los arcos {@code REQUIRES} del catalogo: que articulo no sirve sin cual.
 *
 * <p>
 * <strong>Existe porque esos arcos llevaban desde el changeset 309 sin que los
 * aplicara nadie.</strong> {@code catalog_item_dependencies} tiene nueve
 * {@code REQUIRES} sembrados y un slice CRUD completo en {@code catalogitem}
 * —con su {@code DependencyGraph} y su deteccion de ciclos— pero ni el
 * configurador ni la cotizacion los leian: eran datos declarados y nunca
 * evaluados.
 *
 * <p>
 * <strong>Solo {@code REQUIRES}.</strong> Un {@code RECOMMENDS} es una
 * sugerencia comercial y anadirlo al carrito seria vender de mas sin que nadie
 * lo pidiera; un {@code EXCLUDES} no arrastra nada. Es el mismo criterio con el
 * que {@code DependencyGraph} decide que arco recorrer.
 *
 * <p>
 * {@code catalog_items} y {@code catalog_item_dependencies} son catalogo global
 * de plataforma: no tienen {@code company_id} y no hay empresa que acotar. Solo
 * lee.
 */
public interface CatalogItemDependencyQueryPort {

    /**
     * Articulo -> articulos que necesita para funcionar, ambos {@code ACTIVE} y
     * habilitados.
     *
     * <p>
     * <strong>El grafo entero de una vez, no un salto por consulta.</strong> El
     * cierre es transitivo y son unas decenas de filas: traerlas todas y recorrer
     * en memoria evita el N+1 en un endpoint que sirve a gente sin autenticar, que
     * es donde un N+1 es una via de saturacion gratuita. Es la misma forma que usa
     * {@code GetPublicPlansService} con las lineas de los paquetes.
     *
     * <p>
     * Un requisito que apunte a un articulo retirado no viaja: exigir algo que ya
     * no se vende dejaria el carrito imposible de completar.
     */
    Map<Long, Set<Long>> findRequiredByItemId();
}
