package com.vetsoftware.app.pricelist.domain;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
     * y el ultimo es abierto ({@code tierMax == null}). Una lista sin ningun precio
     * no tiene grupos y por tanto pasa: publicar una tarifa vacia es raro, pero no
     * es un hueco, y convertirlo en uno seria inventarse una regla que la ficha 6
     * no pide.
     *
     * @param prices
     *            TODOS los tramos activos de la lista. Si llega paginado, la
     *            comprobacion es mentira.
     */
    public static void requireFullCoverage(Long priceListId, List<CatalogPrice> prices) {
        for (Map.Entry<Scope, List<CatalogPrice>> group : groupByScope(prices).entrySet()) {
            requireGroupIsContinuous(priceListId, group.getKey(), group.getValue());
        }
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
