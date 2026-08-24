package com.vetsoftware.app.subscription.application.command;

import com.vetsoftware.app.subscription.domain.CapacityUnit;
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
        SubscriptionItemType itemType, CapacityUnit capacityUnit, Integer includedQuantity,
        TaxTreatment taxTreatment, Integer quantity, BigDecimal unitAmount, BigDecimal taxRate,
        LocalDate effectiveFrom, LocalDate effectiveTo) {
}
