package com.vetsoftware.app.accountmapping.domain;

import java.util.Set;

/**
 * Que clase de hecho economico mueve el mapeo. Dominio cerrado y <strong>espejo
 * exacto</strong> de {@code chk_account_mappings_kind} (changeset 343).
 *
 * <h2>Son doce, y el documento maestro daba dos listas incompatibles</h2>
 *
 * <p>
 * La prosa de la capa N enumeraba nueve ({@code CATALOG_ITEM},
 * {@code TAX_OUTPUT}, {@code WITHHOLDING}…) y la seccion «los codigos de cada
 * lista cerrada» enumeraba estas doce. <strong>Manda la segunda</strong>, que
 * es la posterior y la que corrigio la falta de cartera ({@link #RECEIVABLE});
 * es tambien la que escribio el changeset, y un tercer vocabulario seria la
 * divergencia silenciosa que el propio documento persigue.
 *
 * <h2>Solo tres de las doce admiten afinado</h2>
 *
 * <p>
 * {@code chk_account_mappings_refine} exige que {@code catalogItemId},
 * {@code chargeType} y {@code taxTreatment} esten vacios salvo en
 * {@link #REVENUE} y {@link #DEFERRED_REVENUE}: el impuesto generado, la
 * comision de pasarela o el banco <b>no vienen de algo vendido</b> y no tienen
 * articulo al que apuntar. Y {@code chk_account_mappings_deferred} restringe
 * {@code deferredAccountCode} a esas mismas dos.
 */
public enum MappingKind {

    /** Cartera: lo que el cliente debe. */
    RECEIVABLE,

    /** Ingreso diferido: cobrado y aun no devengado. */
    DEFERRED_REVENUE,

    /** Ingreso devengado. */
    REVENUE,

    /** IVA generado, por pagar. */
    VAT_PAYABLE,

    /** IVA descontable. */
    VAT_CREDITABLE,

    /** Efectivo en transito, entre la pasarela y el banco. */
    CASH_IN_TRANSIT,

    /** La cuenta bancaria donde aterriza el giro. */
    BANK,

    /** La comision que se queda la pasarela. */
    GATEWAY_FEE,

    /** La retencion que nos practican: es un activo, no un gasto. */
    WITHHOLDING_ASSET,

    /** El gravamen a los movimientos financieros. */
    FINANCIAL_TAX,

    /** Ingreso por intereses de mora y penalizaciones. */
    PENALTY_REVENUE,

    /** El saldo a favor del cliente. */
    CUSTOMER_CREDIT;

    /**
     * Las dos unicas clases que vienen de algo vendido y por tanto admiten afinado
     * por articulo, tipo de cargo y tratamiento fiscal. Espejo de
     * {@code chk_account_mappings_refine} y de
     * {@code chk_account_mappings_deferred}, que nombran exactamente este par.
     */
    private static final Set<MappingKind> REFINABLE = Set.of(REVENUE, DEFERRED_REVENUE);

    /**
     * {@code true} si la clase admite articulo, tipo de cargo y tratamiento fiscal.
     */
    public boolean acceptsRefinement() {
        return REFINABLE.contains(this);
    }
}
