package com.vetsoftware.app.externalinvoicingoutage.domain;

/**
 * Como salio adelante una clinica concreta durante la caida. Espejo de
 * {@code chk_eioc_resolved}.
 *
 * <p>
 * <strong>El tercero es el que hay que poder demostrar ante la
 * autoridad.</strong> La numeracion de contingencia usada mientras el proveedor
 * externo estaba caido no es un detalle operativo: es el hecho que justifica
 * una serie de documentos emitidos fuera del camino normal, y sin registro de
 * que clinica la uso y en que caida, esa justificacion no existe.
 */
public enum OutageResolution {
    /** El reintento automatico acabo transmitiendo. */
    RETRIED,
    /** Alguien la saco a mano cuando el servicio volvio. */
    MANUAL,
    /**
     * Se emitio con numeracion de contingencia mientras duraba la caida. Es el
     * unico de los tres que hay que poder sostener ante la autoridad.
     */
    CONTINGENCY_NUMBERING
}
