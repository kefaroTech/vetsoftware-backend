package com.vetsoftware.app.entitlement.domain;

/**
 * Por que se recalculo. Companion VO: espeja
 * {@code company_entitlement_snapshots.trigger_reason} sin importar el dominio
 * de {@code companyentitlementsnapshot}.
 *
 * <p>
 * El motivo no es adorno: es lo que permite distinguir "esta clinica perdio
 * acceso porque vencio su prueba" de "lo perdio porque dejo de pagar", y esas
 * dos conversaciones con el cliente no se parecen en nada.
 */
public enum SnapshotReason {

    /** Un otrosi cambio el contrato. */
    CONTRACT_AMENDMENT,

    /** Vencio una prueba y su linea sucesora tomo el relevo. */
    TRIAL_EXPIRED,

    /** La cobranza movio el techo del contrato. */
    DUNNING,

    /** Alguien lo pidio: alta comercial o recalculo a peticion. */
    MANUAL,

    /** Reparacion: los permisos se habian quedado viejos. */
    REPAIR
}
