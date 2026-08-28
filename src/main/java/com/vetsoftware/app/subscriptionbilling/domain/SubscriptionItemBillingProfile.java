package com.vetsoftware.app.subscriptionbilling.domain;

import java.math.BigDecimal;

/**
 * Lo que la linea del contrato le dicta a un cargo que nace de ella: si cobra,
 * y con que impuesto.
 *
 * <p>
 * <b>Existe porque el excedente se facturaba sin IVA.</b>
 * {@code AccrueOverageChargeService} construia el cargo con dos constantes
 * —{@code EXCLUDED} y tarifa {@code 0.00}— porque el tratamiento fiscal de la
 * linea no llegaba hasta alli. El excedente <b>no es un articulo nuevo</b>: es
 * mas consumo del mismo articulo contratado, asi que su impuesto es el de su
 * linea. Con la constante, una linea gravada al 19 % generaba un excedente
 * excluido: <b>una factura emitida de menos ante la DIAN</b>, con la
 * responsabilidad en la empresa que emite y sin ningun sintoma hasta la
 * fiscalizacion.
 *
 * <p>
 * <b>Por que un VO y no dos parametros sueltos en el puerto.</b> El tratamiento
 * y la tarifa <b>solo son validos juntos</b>: {@code TAXED} con tarifa cero es
 * inconstruible aguas abajo —el desglose agregado produciria una fila
 * {@code (TAXED, 0.00)} que {@code chk_sbdt_coherence} rechaza al cerrar el
 * documento—, y {@code EXEMPT} con tarifa distinta de cero tampoco existe.
 * Devolverlos como un par que se valida al nacer es lo que impide que un
 * llamador futuro tome uno y se invente el otro, que es exactamente como
 * aparecio este defecto.
 *
 * <p>
 * <b>La coherencia se comprueba al leer la linea, no al construir el cargo.</b>
 * Es el mismo {@link TaxTreatment#validarTarifa} que aplica
 * {@link SubscriptionCharge}, adelantado un paso: asi un par incoherente en
 * {@code subscription_items} senala la linea que lo tiene mal y no el cargo que
 * lo heredo.
 *
 * @param chargeMode
 *            el modo de cobro de la linea; es quien decide si devenga
 *            (R-TRIAL-14)
 * @param taxRate
 *            copia de {@code subscription_items.tax_rate}
 * @param taxTreatment
 *            copia de {@code subscription_items.tax_treatment}
 */
public record SubscriptionItemBillingProfile(ItemChargeMode chargeMode, BigDecimal taxRate,
        TaxTreatment taxTreatment) {

    public SubscriptionItemBillingProfile {
        if (chargeMode == null)
            throw new IllegalArgumentException("chargeMode is required");
        if (taxTreatment == null)
            throw new IllegalArgumentException("taxTreatment is required");
        taxTreatment.validarTarifa(taxRate);
    }
}
