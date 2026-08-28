package com.vetsoftware.app.subscription.application.dto;

import com.vetsoftware.app.subscription.domain.SubscriptionItemType;
import com.vetsoftware.app.subscription.domain.TaxTreatment;
import java.math.BigDecimal;

/**
 * Valores comerciales resueltos en servidor antes de firmar una línea.
 *
 * <p>
 * Lleva el TRAMO (D-66) porque una cantidad escalonada se firma como varias
 * lineas del mismo articulo a precios distintos, y lleva el DESCUENTO con su
 * marca de condicionado (D-86) porque esa marca viaja congelada desde el
 * renglon de la cotizacion hasta la linea del contrato.
 */
public record SubscriptionItemSnapshot(Long catalogItemId, String itemCode, String itemName,
        SubscriptionItemType itemType, String capacityUnit, int tierMin, Integer tierMax,
        int includedQuantity, TaxTreatment taxTreatment, int quantity, BigDecimal unitAmount,
        BigDecimal discountPercent, BigDecimal discountAmount, boolean discountIsConditional,
        BigDecimal taxRate) {

    /** El snapshot de tramo unico y abierto, sin descuento negociado. */
    public SubscriptionItemSnapshot(Long catalogItemId, String itemCode, String itemName,
            SubscriptionItemType itemType, String capacityUnit, int includedQuantity,
            TaxTreatment taxTreatment, int quantity, BigDecimal unitAmount, BigDecimal taxRate) {
        this(catalogItemId, itemCode, itemName, itemType, capacityUnit, 1, null, includedQuantity,
                taxTreatment, quantity, unitAmount, BigDecimal.ZERO, BigDecimal.ZERO, false,
                taxRate);
    }
}
