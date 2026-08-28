package com.vetsoftware.app.companyentitlementsnapshot.domain;

/**
 * Por qué se recalcularon los permisos. Espeja
 * {@code chk_company_entitlement_snapshots_trigger}.
 */
public enum SnapshotTriggerReason {

    /** Lo movió un otrosí del contrato. Es el único que exige nombrar el papel. */
    CONTRACT_AMENDMENT,

    /** Venció una prueba. */
    TRIAL_EXPIRED,

    /** La cuenta entró o salió de mora. */
    DUNNING,

    /** Lo pidió una persona. */
    MANUAL,

    /** Reparación: los permisos se habían quedado viejos. */
    REPAIR;

    /** Solo el otrosí tiene un papel al que apuntar. */
    public boolean requiresAmendment() {
        return this == CONTRACT_AMENDMENT;
    }
}
