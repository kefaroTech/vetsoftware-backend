package com.vetsoftware.app.configurator.application.port.out;

import com.vetsoftware.app.configurator.domain.BillingCycle;
import com.vetsoftware.app.configurator.domain.PublishedPriceListRef;
import java.util.List;
import java.util.Map;

/**
 * La costura tarifada del configurador: <strong>cuantas unidades de cada eje
 * trae ya puesto el contrato</strong>, para no cobrar dos veces lo que el
 * cliente ya tiene.
 *
 * <p>
 * <strong>Por que el configurador acaba tocando precios.</strong> Resolver una
 * seleccion <em>es</em> una operacion tarifada: la respuesta «quince personas»
 * no vale nada sin saber cuantas vienen incluidas, y ese numero es
 * {@code catalog_prices.included_quantity}, que depende de la tarifa. Lo que no
 * se podia hacer era meterlo en {@code ConfiguratorResolver}: ese es dominio
 * puro y atarlo a una tarifa rompe la separacion que ArchUnit vigila. Vive aqui
 * —capa de aplicacion, puerto explicito— donde el acoplamiento se ve.
 *
 * <p>
 * <strong>El techo de un eje no sale de un solo sitio.</strong> Es
 * {@code included_quantity} del tramo de entrada del articulo {@code is_core}
 * de ese eje <em>mas</em> su {@code min_quantity}, que es la cantidad con la
 * que el contrato inicial lo concede
 * ({@code PlatformCatalogTemplateJpaRepository.findInitialContractTemplate}
 * proyecta {@code ci.min_quantity}). Con la semilla 310: {@code CAPACITY_USER}
 * tiene {@code included_quantity = 1} y {@code min_quantity = 1}, luego el
 * techo es <strong>2</strong> —el dueno y un usuario mas—, que es la cifra que
 * fija el ancla de precio de D-66: quince personas son trece unidades extra, 8
 * x 12.000 + 5 x 9.000 = 141.000.
 *
 * <p>
 * Catalogo global de plataforma: ninguna de las dos tablas tiene
 * {@code company_id}. Solo lee.
 */
public interface CapacityCeilingQueryPort {

    /**
     * Las tarifas {@code PUBLISHED} con su ventana.
     *
     * <p>
     * <strong>La vigencia no se filtra en el SQL a proposito</strong>, igual que en
     * {@code PublicPlanQueryPort.findPublishedPriceLists()}: quien decide que
     * significa «vigente» es {@code PriceListValidity} sobre el reloj inyectado
     * —que lleva la zona del negocio (D-81)— y no un {@code CURRENT_DATE} del
     * motor, que entre las 19:00 y la medianoche ya contesta manana.
     */
    List<PublishedPriceListRef> findPublishedPriceLists();

    /**
     * Eje -> techo que el contrato inicial ya concede, en esa tarifa. El techo
     * depende del ciclo: {@code included_quantity} es columna de la fila de precio
     * y hay una fila por ciclo, asi que preguntarlo sin ciclo seria elegir uno en
     * silencio.
     *
     * <p>
     * Solo los ejes con articulo {@code is_core} tarifado. Un eje ausente del mapa
     * significa «este eje no trae nada incluido», y entonces no hay nada que
     * restar: es el caso de {@code STORAGE_GB}, que se vende entero.
     */
    Map<String, Integer> findStructuralCeilingsByAxis(Long priceListId, BillingCycle billingCycle);
}
