package com.vetsoftware.app.pricelist.application.port.out;

import com.vetsoftware.app.pricelist.domain.CatalogItemRef;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Trae el codigo y el nombre comerciales del articulo, SOLO para pintarlos.
 *
 * <p>
 * Convive con {@link CatalogItemValidationPort} y la division es deliberada: la
 * guarda del camino de escritura -«ese articulo existe»- no necesita ningun
 * dato, y darle uno que traiga columnas la encarece sin motivo. Este puerto
 * existe unicamente porque el camino de LECTURA si los necesita: sin el, el
 * listado de precios de una tarifa devuelve una columna de ids y la consola
 * tiene que cruzar el catalogo en cliente (incidencia #379).
 *
 * <p>
 * {@link #findAllByIds} existe para que el listado no haga N+1: una pagina de
 * veinte precios se resuelve con una consulta, no con veinte.
 *
 * <p>
 * {@code REFERENCIAS_CROSS_FEATURE_ACOTADAS_POR_EMPRESA} no aplica:
 * {@code catalog_items} no tiene {@code company_id}, asi que no hay variante
 * acotada que ofrecer.
 */
public interface CatalogItemQueryPort {

    Optional<CatalogItemRef> findById(Long catalogItemId);

    /**
     * Los articulos pedidos que existan y esten activos, indexados por id. Los que
     * no esten, simplemente no aparecen en el mapa: un precio cuyo articulo se
     * retiro sigue siendo una fila legitima de una tarifa historica y esconderla
     * seria peor que servirla sin nombre.
     */
    Map<Long, CatalogItemRef> findAllByIds(Collection<Long> catalogItemIds);

    /**
     * Los ids de los articulos en estado {@code ACTIVE}.
     *
     * <p>
     * Es el conjunto contra el que R-PRICE-05 mide la cobertura de una tarifa al
     * publicarla. <b>Filtra por {@code status} a diferencia de
     * {@link #findAllByIds}</b>, y la diferencia es deliberada: alli se esta
     * pintando el nombre de una fila historica y esconder un DEPRECATED dejaria la
     * tarifa ilegible; aqui se esta decidiendo que articulos EXIGEN precio, y
     * exigir precio de un articulo retirado impediria publicar cualquier tarifa
     * nueva.
     */
    List<Long> findAllActiveIds();
}
