package com.vetsoftware.app.companylimitevent.domain;

/**
 * De dónde salía el techo en el momento del hecho. Copia propia de esta rodaja.
 *
 * <p>
 * Se guarda <strong>copiado, no referenciado</strong>: dentro de un año el
 * techo habrá cambiado y esta fila tiene que seguir siendo verdad.
 */
public enum LimitSource {
    COMPANY_OVERRIDE, SUBSCRIPTION, CATALOG_DEFAULT, NONE;

    /** Solo el techo negociado nombra la excepción de la que salió. */
    public boolean namesAnOverride() {
        return this == COMPANY_OVERRIDE;
    }
}
