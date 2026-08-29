package com.vetsoftware.app.pricelist.application.port.out;

import com.vetsoftware.app.pricelist.application.dto.PublicPlanComponentRowDto;
import com.vetsoftware.app.pricelist.application.dto.PublicPlanRowDto;
import com.vetsoftware.app.pricelist.application.dto.PublicPriceListDto;
import java.util.List;

/**
 * El read model del catalogo publico. <strong>Solo lee, y solo lo
 * publicable.</strong>
 *
 * <p>
 * Convive con {@link PriceListRepository} y {@link CatalogPriceRepository} y la
 * separacion es el punto entero: aquellos cargan el agregado completo para
 * editarlo, este proyecta las columnas concretas que la landing puede ver. El
 * dia que alguien anada una columna a {@code catalog_prices}, aparecera en el
 * agregado y <strong>no</strong> aqui — que es justo la garantia que se busca,
 * y la que no da un puerto compartido.
 *
 * <p>
 * {@code REFERENCIAS_CROSS_FEATURE_ACOTADAS_POR_EMPRESA} no aplica: ninguna de
 * las cuatro tablas que consulta tiene {@code company_id}.
 */
public interface PublicPlanQueryPort {

    /**
     * Las tarifas {@code PUBLISHED} y habilitadas, con su ventana.
     *
     * <p>
     * <strong>La vigencia no se filtra en el SQL a proposito</strong>, por lo mismo
     * que {@code quote.PriceListQueryPort.findPublishedById}: quien decide que
     * significa «vigente» es
     * {@link com.vetsoftware.app.shared.pricing.PriceListValidity}, el unico
     * predicado del arbol, y la fecha se deriva del reloj inyectado —que lleva la
     * zona del negocio (D-81)— y no de un {@code CURRENT_DATE} del motor, que entre
     * las 19:00 y la medianoche ya contesta manana y dejaria la portada sin precios
     * el ultimo dia de la tarifa.
     */
    List<PublicPriceListDto> findPublishedPriceLists();

    /** Los paquetes vendibles con precio de entrada en esa tarifa. */
    List<PublicPlanRowDto> findPlans(Long priceListId);

    /** Las lineas de esos paquetes, resueltas contra esa misma tarifa. */
    List<PublicPlanComponentRowDto> findPlanComponents(Long priceListId);
}
