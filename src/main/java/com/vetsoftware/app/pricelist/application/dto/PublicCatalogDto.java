package com.vetsoftware.app.pricelist.application.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Todo lo contratable de la tarifa vigente, repartido por naturaleza.
 *
 * <p>
 * Una lista por naturaleza y no una sola con un discriminante porque los grupos
 * se pintan distinto y llevan campos distintos: un contador tiene unidad y
 * unidades incluidas, un paquete tiene composicion, un cargo unico no tiene
 * ninguna de las dos. Una lista unica obligaria a que la mitad de los campos
 * fueran nulos por construccion, que es la forma de no poder distinguir «no
 * aplica» de «no hay dato» — la distincion que esta respuesta existe para
 * conservar.
 *
 * <p>
 * {@code currency} y {@code priceValidFrom} nulos con todas las listas vacias
 * es la respuesta 200 valida cuando no hay tarifa vigente, igual que en
 * {@link PublicPlanCatalogDto}: la portada tiene que seguir cargando.
 *
 * @param requirements
 *            el grafo «si eliges esto, se te anade aquello», <strong>como lista
 *            de arcos y no como campo dentro de cada articulo</strong>. Va
 *            arriba por dos razones. La primera es de contrato: los tres
 *            records de articulo —{@code PublicCatalogItemResponse},
 *            {@code PublicCatalogCapacityResponse},
 *            {@code PublicCatalogPackResponse}— quedan <em>intactos</em>, asi
 *            que los dos fronts declaran un tipo nuevo y no tocan tres que ya
 *            funcionan. La segunda es de completitud: un arco es un arco
 *            independientemente del tipo de sus extremos, mientras que repartir
 *            el campo por tipo de articulo obliga a decidir donde cuelga un
 *            requisito cuyo origen sea un {@code BUNDLE} —hoy no hay ninguno, y
 *            el dia que lo haya se caeria en silencio—. Ver
 *            {@link PublicCatalogRequirementRowDto} para por que son arcos
 *            directos y no el cierre transitivo. Lista vacia, nunca nula.
 * @param areas
 *            las cabeceras funcionales bajo las que se agrupan los modulos, en
 *            el orden en que se pintan. <strong>Viene vacia cuando no hay
 *            tarifa vigente, aunque {@code catalog_areas} no dependa de la
 *            tarifa.</strong> Sin tarifa {@code modules} esta vacia entera, y
 *            una cabecera sin un solo modulo debajo es un titulo sobre la nada.
 */
public record PublicCatalogDto(String currency, LocalDate priceValidFrom,
        List<PublicCatalogItemDto> modules, List<PublicCatalogCapacityDto> capacities,
        List<PublicCatalogItemDto> oneTimeItems, List<PublicCatalogPackDto> packs,
        List<PublicCatalogRequirementDto> requirements, List<PublicCatalogAreaDto> areas) {
}
