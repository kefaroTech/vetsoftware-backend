package com.vetsoftware.app.subscription.application.dto;

import com.vetsoftware.app.subscription.domain.SubscriptionItemType;
import com.vetsoftware.app.subscription.domain.TaxTreatment;
import java.math.BigDecimal;

/**
 * El minimo estructural de la plataforma, ya resuelto: con esto y solo con esto
 * se puede firmar el contrato inicial de una empresa nueva.
 *
 * <p>
 * Es una <strong>foto</strong>, y esa es toda su razon de ser. Los valores que
 * trae —precio unitario, IVA, tratamiento fiscal y sobre todo lo incluido— se
 * copian a la {@code subscription_items} en el momento del alta y no se vuelven
 * a leer del catalogo: a partir de ahi la tarifa puede cambiar cuanto quiera,
 * que este cliente ya no la mira.
 */
public record InitialContractTemplate(Long priceListId, Long catalogItemId, String itemCode,
        String itemName, SubscriptionItemType itemType, String capacityUnit, int includedQuantity,
        int minQuantity, BigDecimal unitAmount, BigDecimal taxRate, TaxTreatment taxTreatment,
        int defaultGraceDays, int defaultTrialDays) {
}
