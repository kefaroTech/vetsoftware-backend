package com.vetsoftware.app.subscription.application.dto;

import com.vetsoftware.app.subscription.domain.SubscriptionItemType;
import com.vetsoftware.app.subscription.domain.TaxTreatment;
import java.math.BigDecimal;

/**
 * Un articulo {@code trial_eligibility = 'ELIGIBLE'} ya resuelto para el ciclo
 * pedido: los módulos comerciales más las capacidades del catalogo que tambien
 * se prueban. {@code ELECTRONIC_INVOICING} ({@code NEVER_FREE}) nunca aparece
 * aqui.
 *
 * <p>
 * Es una <strong>foto</strong>, con la misma razon de ser que
 * {@link InitialContractTemplate}: el precio, el IVA y lo incluido se copian a
 * la {@code subscription_items} en el momento del alta y no se vuelven a leer
 * del catalogo.
 *
 * <p>
 * {@code trialOutcome} viaja tambien aqui aunque {@code subscription_items} no
 * lo guarde: es lo unico que necesita la concesion de prueba
 * ({@code companytrialgrant}), y esta fila es la unica fuente de "que es
 * elegible" en toda la plataforma — sin este campo, quien concede la prueba
 * tendria que volver a consultar el catalogo por su cuenta.
 */
public record EligibleTrialItemTemplate(Long catalogItemId, String itemCode, String itemName,
        SubscriptionItemType itemType, String capacityUnit, int includedQuantity, int minQuantity,
        BigDecimal unitAmount, BigDecimal taxRate, TaxTreatment taxTreatment, int defaultTrialDays,
        String trialOutcome) {
}
