package com.vetsoftware.app.companybillingprofile.domain;

/**
 * Regimen fiscal del tercero al que se le factura. Dominio cerrado y espejo
 * <strong>literal</strong> de {@code chk_company_billing_profiles_tax_regime}.
 *
 * <p>
 * <strong>Se llama igual que {@code companytaxprofile.domain.TaxRegime},
 * {@code owner.domain.TaxRegime} y {@code electronicdocument.domain.TaxRegime},
 * y son cuatro enums distintos a proposito.</strong> Aquellos describen el
 * regimen de la <em>propia clinica</em> como emisora de facturas electronicas y
 * distinguen dos valores; este describe el del <em>cliente al que VetSoftware
 * le factura</em> y tiene cuatro, que son las cuatro que admite el
 * {@code CHECK} de esta tabla. Compartirlos —o importar el de la otra feature—
 * seria exactamente lo que prohibe el vertical slicing, y ademas ataria dos
 * listas cerradas que cambian con normas distintas.
 */
public enum TaxRegime {

    /** Regimen ordinario. */
    COMMON,

    /** Regimen simple de tributacion. */
    SIMPLE,

    /** No responsable de IVA. */
    NOT_RESPONSIBLE_VAT,

    /** Regimen tributario especial. */
    SPECIAL
}
