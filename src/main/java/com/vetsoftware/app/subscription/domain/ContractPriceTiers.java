package com.vetsoftware.app.subscription.domain;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Parte una cantidad contratada entre los tramos que le corresponden,
 * <b>acumulativamente</b> (D-66, R-PRICE-04).
 *
 * <p>
 * Es la mitad "contrato" de la misma cuenta que {@code TieredPrice} hace en la
 * cotizacion: con "unidades extra 1 a 8 a 12.000 y de la 9 en adelante a
 * 9.000", trece unidades son ocho a 12.000 mas cinco a 9.000 —141.000— y no
 * trece al precio del tramo alto —117.000—. La duplicacion entre los dos slices
 * es deliberada y es la regla de rodajas verticales del repositorio: ninguno
 * importa el dominio del otro.
 *
 * <p>
 * <b>Los tramos se indexan sobre lo FACTURABLE.</b> Lo que la tarifa escalona
 * son las unidades que se cobran, asi que lo incluido se resta primero y el
 * reparto empieza en la unidad uno de las que quedan.
 */
public final class ContractPriceTiers {

    private ContractPriceTiers() {
    }

    /**
     * @param allTiers
     *            TODOS los tramos del articulo en esa tarifa y ciclo. Si llega
     *            recortado -por ejemplo, solo el que cubre la cantidad-, el reparto
     *            es mentira y vuelve a salir el precio plano.
     * @return una entrada por tramo con al menos una unidad, en orden. Nunca vacio:
     *         si lo contratado no supera lo incluido, devuelve el primer tramo con
     *         cantidad igual a lo incluido, porque una linea de contrato con
     *         {@code quantity = 0} no existe
     *         -{@code chk_subscription_items_quantity} la rechaza- y el cliente
     *         sigue teniendo derecho a lo que le viene incluido.
     */
    public static List<ContractTierLine> allocate(int contractedQuantity,
            List<ContractPriceTier> allTiers) {
        if (allTiers == null || allTiers.isEmpty())
            throw new IllegalArgumentException("at least one price tier is required");
        if (contractedQuantity < 1)
            throw new IllegalArgumentException("contractedQuantity must be greater than zero");
        List<ContractPriceTier> ordered = new ArrayList<>(allTiers);
        ordered.sort(Comparator.comparingInt(ContractPriceTier::tierMin));
        ContractPriceTier first = ordered.get(0);
        if (first.tierMin() != 1)
            throw new IllegalArgumentException(
                    "price tiers must start at 1, got " + first.tierMin());
        int included = Math.min(first.includedQuantity(), contractedQuantity);
        int billable = contractedQuantity - included;
        if (billable == 0)
            return List.of(new ContractTierLine(first, included, included));
        List<ContractTierLine> lines = new ArrayList<>();
        for (ContractPriceTier tier : ordered) {
            int units = tier.unitsWithin(billable);
            if (units == 0)
                continue;
            // Lo incluido se suma UNA sola vez, al tramo que arranca en uno: la linea
            // factura quantity menos includedQuantity, asi que repetirlo lo regalaria una
            // vez por tramo.
            boolean firstTier = lines.isEmpty();
            lines.add(new ContractTierLine(tier, firstTier ? units + included : units,
                    firstTier ? included : 0));
        }
        return List.copyOf(lines);
    }
}
