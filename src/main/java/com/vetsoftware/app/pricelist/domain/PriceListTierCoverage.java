package com.vetsoftware.app.pricelist.domain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Comprueba que los tramos de precio de una lista cubren TODAS las cantidades.
 *
 * <p>
 * Sin esto, un comercial puede montar {@code EXTRA_USER} a 12.000 del usuario 1
 * al 10 y a 9.000 del 21 en adelante, olvidarse del tramo intermedio y
 * publicar. Nada se queja: {@code uq_catalog_prices_tier} solo mira
 * {@code tier_min}, {@link CatalogPrice#requireNoTierOverlap} solo mira que no
 * se pisen y MySQL no sabe expresar cobertura. Un cliente contrata 15 usuarios
 * y la consulta de resolucion
 * -{@code tier_min <= 15 ORDER BY tier_min DESC LIMIT 1}- devuelve el tramo
 * {@code [1,10]}, que no le corresponde, o no devuelve nada. Las dos salidas
 * cotizan mal y ninguna hace ruido: el error aparece en la factura del cliente,
 * no en el build. Incidencia #378.
 *
 * <p>
 * Es {@code static} y sin estado a proposito: es una invariante del dominio, y
 * ponerla en el servicio la dejaria fuera del alcance de cualquiera que
 * construya una lista por otro camino.
 */
public final class PriceListTierCoverage {

    private PriceListTierCoverage() {
    }

    /**
     * Lanza {@link CatalogPriceTierGapException} en el PRIMER hueco que encuentre,
     * recorriendo los grupos en orden de {@code (catalogItemId, billingCycle)} para
     * que dos ejecuciones sobre los mismos datos senalen siempre el mismo. Un
     * mensaje reproducible es la diferencia entre corregir una tarifa y
     * perseguirla.
     *
     * <p>
     * Un grupo esta cubierto si sus tramos ordenados por {@code tierMin} arrancan
     * en 1, encadenan sin salto -{@code siguiente.tierMin == anterior.tierMax + 1}-
     * y el ultimo es abierto ({@code tierMax == null}).
     *
     * <p>
     * <b>La ausencia total la caza {@link #requireEveryActiveItemPriced} y no
     * esto.</b> Esta mitad agrupa sobre los precios escritos, asi que un articulo
     * sin ninguna fila no produce grupo y pasaba limpio: publicar una tarifa a la
     * que le falta el nucleo entero era legal, y despues ninguna empresa podia
     * registrarse. Contrastar contra los articulos activos es lo que cierra ese
     * agujero (R-PRICE-05, defecto construido #16).
     *
     * @param prices
     *            TODOS los tramos activos de la lista. Si llega paginado, la
     *            comprobacion es mentira.
     * @param activeCatalogItemIds
     *            los articulos {@code ACTIVE} del catalogo, que es el conjunto
     *            contra el que se mide la cobertura.
     */
    public static void requireFullCoverage(Long priceListId, List<CatalogPrice> prices,
            Collection<Long> activeCatalogItemIds) {
        Map<Scope, List<CatalogPrice>> groups = groupByScope(prices);
        requireEveryActiveItemPriced(priceListId, groups.keySet(), activeCatalogItemIds);
        for (Map.Entry<Scope, List<CatalogPrice>> group : groups.entrySet()) {
            requireGroupIsContinuous(priceListId, group.getKey(), group.getValue());
        }
    }

    /**
     * R-PRICE-05: <b>contra los articulos ACTIVOS, no contra la continuidad de lo
     * escrito</b>.
     *
     * <p>
     * Se recorren los articulos y no los grupos, y ese es todo el arreglo. La
     * continuidad solo sabe mirar lo que existe: un articulo sin ninguna fila no
     * produce grupo, no produce hueco, y la lista se publica limpia con un articulo
     * entero sin tarifar. Si el olvidado es el nucleo, ninguna empresa puede
     * registrarse y nada relaciona el registro roto de manana con la publicacion de
     * hoy.
     *
     * <p>
     * Se exige <b>al menos un precio</b>, no uno por ciclo: pedir los dos ciclos
     * seria una regla que ninguna ficha declara y rechazaria tarifas legitimas de
     * un solo ciclo. Lo que se persigue aqui es la ausencia TOTAL, que es la que no
     * deja rastro; una ausencia parcial de ciclo la caza el alta al no encontrar
     * precio, con el nombre del articulo delante.
     *
     * <p>
     * Los articulos se recorren en orden ascendente para que dos ejecuciones sobre
     * los mismos datos senalen siempre el mismo, por el mismo motivo que los
     * grupos.
     *
     * @param activeCatalogItemIds
     *            los articulos en estado {@code ACTIVE}. Vacio o nulo desactiva la
     *            comprobacion: no hay contra que contrastar, y inventarse que
     *            "ninguno activo" significa "todo bien" es preferible a rechazar
     *            toda publicacion si el puerto falla en devolverlos.
     */
    private static void requireEveryActiveItemPriced(Long priceListId, Collection<Scope> scopes,
            Collection<Long> activeCatalogItemIds) {
        if (activeCatalogItemIds == null || activeCatalogItemIds.isEmpty())
            return;
        Set<Long> priced = scopes.stream().map(Scope::catalogItemId).collect(Collectors.toSet());
        activeCatalogItemIds.stream().filter(java.util.Objects::nonNull).distinct().sorted()
                .filter(itemId -> !priced.contains(itemId)).findFirst().ifPresent(itemId -> {
                    throw new CatalogPriceMissingForActiveItemException(priceListId, itemId);
                });
    }

    private static Map<Scope, List<CatalogPrice>> groupByScope(List<CatalogPrice> prices) {
        // LinkedHashMap sobre una entrada ya ordenada: el orden de los grupos es el de
        // (articulo, ciclo) y no el del hash, que cambia entre ejecuciones.
        List<CatalogPrice> ordered = prices == null ? new ArrayList<>() : new ArrayList<>(prices);
        ordered.sort(Comparator.comparing(CatalogPrice::getCatalogItemId)
                .thenComparing(price -> price.getBillingCycle().name())
                .thenComparingInt(CatalogPrice::getTierMin));
        Map<Scope, List<CatalogPrice>> grouped = new LinkedHashMap<>();
        for (CatalogPrice price : ordered) {
            grouped.computeIfAbsent(new Scope(price.getCatalogItemId(), price.getBillingCycle()),
                    scope -> new ArrayList<>()).add(price);
        }
        return grouped;
    }

    private static void requireGroupIsContinuous(Long priceListId, Scope scope,
            List<CatalogPrice> tiers) {
        CatalogPrice first = tiers.get(0);
        if (first.getTierMin() > 1) {
            throw gap(priceListId, scope, 1, first.getTierMin() - 1);
        }
        for (int i = 0; i < tiers.size() - 1; i++) {
            Integer previousMax = tiers.get(i).getTierMax();
            // previousMax nulo con un tramo detras es un solape, no un hueco, y lo rechaza
            // requireNoTierOverlap mucho antes de llegar aqui. Se deja pasar en vez de
            // reportarlo como lo que no es: un diagnostico equivocado manda a corregir el
            // sitio equivocado.
            if (previousMax == null) {
                return;
            }
            int nextMin = tiers.get(i + 1).getTierMin();
            if (nextMin > previousMax + 1) {
                throw gap(priceListId, scope, previousMax + 1, nextMin - 1);
            }
        }
        Integer lastMax = tiers.get(tiers.size() - 1).getTierMax();
        if (lastMax != null) {
            throw gap(priceListId, scope, lastMax + 1, null);
        }
    }

    private static CatalogPriceTierGapException gap(Long priceListId, Scope scope, int from,
            Integer to) {
        return new CatalogPriceTierGapException(priceListId, scope.catalogItemId(),
                scope.billingCycle(), from, to);
    }

    /** El alcance dentro del cual los tramos tienen que encadenar. */
    private record Scope(Long catalogItemId, BillingCycle billingCycle) {
    }
}
