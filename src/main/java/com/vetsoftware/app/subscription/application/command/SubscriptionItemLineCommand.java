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
 *
 * <p>
 * {@code chargeMode}, {@code trialEligibility}, {@code maxTrialDays},
 * {@code trialEndDate} y {@code activationPath} son las columnas del changeset
 * 244; el constructor de tramo único las fija a los mismos valores por defecto
 * que {@code SubscriptionItemJpaEntity}.
 */
public record SubscriptionItemLineCommand(Long catalogItemId, String itemCode, String itemName,
        SubscriptionItemType itemType, String capacityUnit, Integer tierMin, Integer tierMax,
        Integer includedQuantity, TaxTreatment taxTreatment, Integer quantity,
        BigDecimal unitAmount, BigDecimal discountPercent, BigDecimal discountAmount,
        boolean discountIsConditional, BigDecimal taxRate, LocalDate effectiveFrom,
        LocalDate effectiveTo, String chargeMode, String trialEligibility, int maxTrialDays,
        LocalDate trialEndDate, String activationPath) {

    /**
     * {@code chk_subscription_items_charge_mode}: lo que cobra hoy todo lo que no
     * prueba.
     */
    public static final String DEFAULT_CHARGE_MODE = "PAID";
    /**
     * {@code chk_subscription_items_trial_eligibility}: nunca se regala salvo que
     * se diga.
     */
    public static final String DEFAULT_TRIAL_ELIGIBILITY = "NEVER_FREE";
    /**
     * {@code chk_subscription_items_activation_path}: canal por defecto de
     * plataforma.
     */
    public static final String DEFAULT_ACTIVATION_PATH = "PLATFORM";

    /** La linea de tramo unico y abierto, sin descuento negociado ni prueba. */
    public SubscriptionItemLineCommand(Long catalogItemId, String itemCode, String itemName,
            SubscriptionItemType itemType, String capacityUnit, Integer includedQuantity,
            TaxTreatment taxTreatment, Integer quantity, BigDecimal unitAmount, BigDecimal taxRate,
            LocalDate effectiveFrom, LocalDate effectiveTo) {
        this(catalogItemId, itemCode, itemName, itemType, capacityUnit, 1, null, includedQuantity,
                taxTreatment, quantity, unitAmount, null, null, false, taxRate, effectiveFrom,
                effectiveTo, DEFAULT_CHARGE_MODE, DEFAULT_TRIAL_ELIGIBILITY, 0, null,
                DEFAULT_ACTIVATION_PATH);
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
