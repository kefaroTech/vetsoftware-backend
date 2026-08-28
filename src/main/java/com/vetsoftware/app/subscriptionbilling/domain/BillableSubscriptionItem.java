package com.vetsoftware.app.subscriptionbilling.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Una línea del contrato vista <b>desde la caja</b>: lo justo para decidir si
 * devenga y por cuánto.
 *
 * <p>
 * <b>Companion VO.</b> No es {@code SubscriptionItem}: el vertical slicing
 * prohíbe importar el dominio de {@code subscription}, y además esta rodaja no
 * necesita ni la mitad de sus campos.
 *
 * <p>
 * <b>El filtro es {@link #chargeMode}, y nunca el estado del contrato</b>
 * (R-TRIAL-13). Un contrato en {@code TRIALING} tiene líneas {@code PAID} —la
 * facturación electrónica DIAN se cobra desde el día 0— y descartar el contrato
 * entero por su estado deja de facturar servicios realmente prestados. Son
 * cincuenta y nueve mil pesos al mes por cliente que no se cobran y que nadie
 * ve faltar, porque la factura sale bien formada, solo que más corta.
 */
public record BillableSubscriptionItem(Long id, Long companyId, Long subscriptionId,
        Long catalogItemId, String itemName, ItemChargeMode chargeMode, int quantity,
        int includedQuantity, BigDecimal unitAmount, BigDecimal taxRate, TaxTreatment taxTreatment,
        LocalDate effectiveFrom, LocalDate effectiveTo) {

    public BillableSubscriptionItem {
        if (id == null)
            throw new IllegalArgumentException("subscription item id is required");
        if (companyId == null)
            throw new IllegalArgumentException("subscription item companyId is required");
        if (subscriptionId == null)
            throw new IllegalArgumentException("subscription item subscriptionId is required");
        if (itemName == null || itemName.isBlank())
            throw new IllegalArgumentException("subscription item name is required");
        if (chargeMode == null)
            throw new IllegalArgumentException("subscription item chargeMode is required");
        if (quantity < 0)
            throw new IllegalArgumentException("quantity cannot be negative");
        if (includedQuantity < 0)
            throw new IllegalArgumentException("includedQuantity cannot be negative");
        if (unitAmount == null || unitAmount.signum() < 0)
            throw new IllegalArgumentException("unitAmount cannot be negative");
        if (taxTreatment == null)
            throw new IllegalArgumentException("taxTreatment is required");
    }

    /**
     * ¿Esta línea devenga en este barrido?
     *
     * <p>
     * Dos condiciones y ninguna mira el contrato: que el modo de cobro sea
     * {@code PAID} y que la línea esté viva el día en que arranca el periodo.
     */
    public boolean devenga(LocalDate periodStart) {
        return chargeMode.generatesCharge() && vigenteEn(periodStart);
    }

    /**
     * Vigencia semiabierta {@code [effectiveFrom, effectiveTo)}, como la columna.
     */
    public boolean vigenteEn(LocalDate day) {
        if (effectiveFrom != null && day.isBefore(effectiveFrom))
            return false;
        return effectiveTo == null || day.isBefore(effectiveTo);
    }

    /**
     * Lo que de verdad se cobra: {@code max(cantidad − incluido, 0)}.
     *
     * <p>
     * Mismo criterio que usa el prorrateo de un otrosí, escrito con la misma
     * fórmula: si las dos difirieran, un alta y su baja del mismo día dejarían de
     * sumar cero.
     */
    public int billableQuantity() {
        return Math.max(quantity - includedQuantity, 0);
    }

    /**
     * La base del cargo del periodo, sin impuesto. El IVA lo desglosa el documento
     * sobre la base agregada y no se guarda aquí (ver {@link SubscriptionCharge}).
     */
    public BigDecimal recurringSubtotal() {
        return unitAmount.multiply(BigDecimal.valueOf(billableQuantity()));
    }
}
