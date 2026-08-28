package com.vetsoftware.app.quote.domain;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Parte una cantidad contratada entre los tramos que le corresponden,
 * <b>acumulativamente</b> (D-66, R-PRICE-04).
 *
 * <p>
 * <b>La cuenta que este tipo existe para hacer bien.</b> Con "unidades extra 1
 * a 8 a 12.000 y de la 9 en adelante a 9.000", quince usuarios son trece
 * unidades extra -el nucleo trae dos- y se cobran <b>ocho a 12.000 mas cinco a
 * 9.000 = 141.000</b>. La aritmetica plana -tomar el tramo mas alto que cubra
 * la cantidad y multiplicar todo por el- daba 117.000: veinticuatro mil por
 * cliente y mes, unos diecisiete millones al ano a sesenta clinicas, sin un
 * error y sin una alarma. Ese es el defecto que este tipo cierra, y por eso el
 * reparto vive en el dominio y no en el servicio: ahi lo comprueba
 * {@link QuoteLine#tierQuantity} en cada construccion, lectura incluida.
 *
 * <p>
 * <b>Los tramos se indexan sobre lo FACTURABLE, no sobre lo contratado.</b> Lo
 * que la tarifa escalona son las unidades que se cobran, asi que lo incluido se
 * resta primero (R15) y el reparto empieza en la unidad uno de las que quedan.
 * "Usuarios 3 a 10" son las unidades extra 1 a 8, que es exactamente la
 * traduccion que hace cuadrar D-66.
 *
 * @param includedQuantity
 *            lo que el ARTICULO incluye, tomado del tramo que arranca en uno.
 *            Es propiedad del articulo y no del tramo: leerla del tramo alto
 *            haria que contratar mas unidades cambiase cuantas vienen de
 *            regalo.
 * @param tiers
 *            los tramos que reciben al menos una unidad, en orden. Vacio cuando
 *            lo contratado no supera lo incluido: entonces no hay nada que
 *            cobrar y no se emite ninguna linea.
 */
public record TieredPrice(int includedQuantity, List<CatalogPriceRef> tiers) {

    public TieredPrice {
        tiers = tiers == null ? List.of() : List.copyOf(tiers);
    }

    /**
     * @param allTiers
     *            TODOS los tramos del articulo en esa tarifa y ciclo. Si llega
     *            recortado -por ejemplo, solo el tramo que cubre la cantidad-, el
     *            reparto es mentira y vuelve a salir el precio plano.
     */
    public static TieredPrice of(QuoteItemType itemType, int contractedQuantity,
            List<CatalogPriceRef> allTiers) {
        if (allTiers == null || allTiers.isEmpty())
            throw new IllegalArgumentException("at least one price tier is required");
        List<CatalogPriceRef> ordered = new ArrayList<>(allTiers);
        ordered.sort(Comparator.comparingInt(CatalogPriceRef::tierMin));
        CatalogPriceRef first = ordered.get(0);
        if (first.tierMin() != 1)
            throw new IllegalArgumentException(
                    "price tiers must start at 1, got " + first.tierMin());
        int included = first.includedQuantity();
        int billable = QuoteLine.billableQuantity(itemType, contractedQuantity, included);
        List<CatalogPriceRef> applicable = new ArrayList<>();
        for (CatalogPriceRef tier : ordered) {
            if (tier.unitsWithin(billable) > 0)
                applicable.add(tier);
        }
        return new TieredPrice(included, applicable);
    }
}
