package com.vetsoftware.app.pricelist.application.port.out;

import com.vetsoftware.app.pricelist.application.dto.LinkStateDto;
import com.vetsoftware.app.pricelist.domain.BillingCycle;
import com.vetsoftware.app.pricelist.domain.CatalogPrice;
import com.vetsoftware.app.shared.pagination.PageResult;
import java.util.List;
import java.util.Optional;

public interface CatalogPriceRepository {

    CatalogPrice save(CatalogPrice catalogPrice);

    Optional<CatalogPrice> findById(Long id);

    PageResult<CatalogPrice> findAllByPriceListId(Long priceListId, int page, int pageSize);

    /**
     * Los precios que comparten {@code (lista, articulo, ciclo)} con el candidato:
     * el conjunto exacto contra el que hay que comprobar el solape de tramos.
     *
     * <p>
     * No esta paginado a proposito. Es el insumo de una invariante, no un listado
     * de interfaz: paginarlo dejaria fuera del examen justo al hermano que se pisa
     * con el candidato. Cabe de sobra en memoria -son los tramos de un articulo en
     * una lista, unidades por escenario-.
     */
    List<CatalogPrice> findTierScope(Long priceListId, Long catalogItemId,
            BillingCycle billingCycle);

    /**
     * TODOS los tramos activos de la lista, sin paginar y sin acotar por articulo.
     *
     * <p>
     * Es el insumo de {@code PriceListTierCoverage}, que comprueba al publicar que
     * los tramos de cada {@code (articulo, ciclo)} cubren todas las cantidades
     * (incidencia #378). No se puede paginar por la misma razon que
     * {@link #findTierScope}, solo que peor: una pagina que corte un grupo por la
     * mitad convierte la comprobacion de cobertura en una mentira que ademas
     * rechaza tarifas correctas. Son los tramos de una tarifa entera -decenas de
     * filas por escenario-, y se leen una vez en la vida de la lista.
     */
    List<CatalogPrice> findAllTiers(Long priceListId);

    /**
     * El tramo exacto {@code (lista, articulo, ciclo, tierMin)} <strong>ignorando
     * el borrado logico</strong>, que es como lo mira
     * {@code uq_catalog_prices_tier}. {@link #findTierScope} no sirve para esto:
     * solo ve los activos, y es correcto que asi sea porque un tramo retirado no
     * compite por ninguna unidad — pero si sigue ocupando la clave. Ver
     * {@link LinkStateDto}.
     */
    Optional<LinkStateDto> findAnyByTier(Long priceListId, Long catalogItemId,
            BillingCycle billingCycle, int tierMin);

    /** Deshace la baja logica. Devuelve las filas afectadas (0 o 1). */
    int reactivate(Long id);

    long countByPriceListId(Long priceListId);

    void delete(Long id);
}
