package com.vetsoftware.app.platformtaxprofile.domain;

/**
 * El regimen de IVA de VetSoftware.
 *
 * <p>
 * <strong>La base NO impone esta lista.</strong> {@code tax_regime} es un
 * {@code VARCHAR(30)} sin {@code CHECK} —el changeset 367 lo dice: «sin CHECK
 * sobre document_type ni tax_regime porque 116 tampoco lo tiene»—, asi que la
 * lista cerrada vive aqui y solo aqui.
 *
 * <p>
 * <strong>Los dos valores son los de
 * {@code companytaxprofile.domain.TaxRegime}, palabra por palabra.</strong> El
 * regimen no cambia de significado segun quien lo declare: responsable de IVA
 * es responsable de IVA lo emita una clinica o lo emita VetSoftware. Escribir
 * aqui {@code IVA_RESPONSIBLE} o {@code COMUN} crearia un segundo vocabulario
 * para el mismo concepto —el defecto que el documento de esquema persigue tabla
 * por tabla— y haria incomparables dos columnas que un dia habra que cotejar.
 */
public enum PlatformTaxRegime {

    /** Responsable de IVA. Cobra IVA en sus facturas y lo declara. */
    RESPONSABLE_IVA,

    /** No responsable de IVA. */
    NO_RESPONSABLE_IVA
}
