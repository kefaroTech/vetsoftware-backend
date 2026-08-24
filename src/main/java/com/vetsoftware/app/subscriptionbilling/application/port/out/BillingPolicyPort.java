package com.vetsoftware.app.subscriptionbilling.application.port.out;

/**
 * La política de cobro de la plataforma, de la que este slice solo necesita una
 * cosa: cuántos días de plazo tiene el cliente para pagar.
 *
 * <p>
 * Es global —{@code platform_billing_config} es un singleton sin empresa— y por
 * eso el puerto no recibe {@code companyId}: no hay ninguno que pasarle.
 */
public interface BillingPolicyPort {

    /**
     * Días de plazo por defecto, contados <b>desde la fecha fiscal</b> de la
     * factura externa. Ver
     * {@code SubscriptionBillingDocument#registerExternalInvoice}.
     */
    int defaultPaymentTermDays();
}
