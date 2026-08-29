package com.vetsoftware.app.pricelist.application.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Todo lo contratable de la tarifa vigente, repartido por naturaleza.
 *
 * <p>
 * Cuatro listas y no una con un discriminante porque los cuatro grupos se
 * pintan distinto y llevan campos distintos: un contador tiene unidad y
 * unidades incluidas, un paquete tiene composicion, un cargo unico no tiene
 * ninguna de las dos. Una lista unica obligaria a que la mitad de los campos
 * fueran nulos por construccion, que es la forma de no poder distinguir «no
 * aplica» de «no hay dato» — la distincion que esta respuesta existe para
 * conservar.
 *
 * <p>
 * {@code currency} y {@code priceValidFrom} nulos con las cuatro listas vacias
 * es la respuesta 200 valida cuando no hay tarifa vigente, igual que en
 * {@link PublicPlanCatalogDto}: la portada tiene que seguir cargando.
 */
public record PublicCatalogDto(String currency, LocalDate priceValidFrom,
        List<PublicCatalogItemDto> modules, List<PublicCatalogCapacityDto> capacities,
        List<PublicCatalogItemDto> oneTimeItems, List<PublicCatalogPackDto> packs) {
}
