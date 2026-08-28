package com.vetsoftware.app.subscription.application.command;

import com.vetsoftware.app.subscription.domain.SubscriptionItemType;
import com.vetsoftware.app.subscription.domain.TaxTreatment;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Una linea que se firma. Los seis campos congelados —codigo, nombre, tipo,
 * unidad, tratamiento fiscal, precio, IVA y lo incluido— llegan resueltos desde
 * la tarifa por quien firma (aceptacion de cotizacion o consola de plataforma)
 * y este slice los <strong>copia</strong> a la fila: a partir de ahi el cliente
 * ya no mira la tarifa, que es lo que impide que editar un tramo le cambie
 * retroactivamente lo que le sobra.
 */
public record SubscriptionItemLineCommand(Long catalogItemId, String itemCode, String itemName,
        SubscriptionItemType itemType, String capacityUnit, Integer tierMin, Integer tierMax,
        Integer includedQuantity, TaxTreatment taxTreatment, Integer quantity,
        BigDecimal unitAmount, BigDecimal discountPercent, BigDecimal discountAmount,
        boolean discountIsConditional, BigDecimal taxRate, LocalDate effectiveFrom,
        LocalDate effectiveTo) {

    /** La linea de tramo unico y abierto, sin descuento negociado. */
    public SubscriptionItemLineCommand(Long catalogItemId, String itemCode, String itemName,
            SubscriptionItemType itemType, String capacityUnit, Integer includedQuantity,
            TaxTreatment taxTreatment, Integer quantity, BigDecimal unitAmount, BigDecimal taxRate,
            LocalDate effectiveFrom, LocalDate effectiveTo) {
        this(catalogItemId, itemCode, itemName, itemType, capacityUnit, 1, null, includedQuantity,
                taxTreatment, quantity, unitAmount, null, null, false, taxRate, effectiveFrom,
                effectiveTo);
    }

    /** El tramo unico y abierto de un articulo sin escalones. */
    public int tierMinOrDefault() {
        return tierMin == null ? 1 : tierMin;
    }

    public BigDecimal discountPercentOrZero() {
        return discountPercent == null ? BigDecimal.ZERO : discountPercent;
    }

    public BigDecimal discountAmountOrZero() {
        return discountAmount == null ? BigDecimal.ZERO : discountAmount;
    }
}
