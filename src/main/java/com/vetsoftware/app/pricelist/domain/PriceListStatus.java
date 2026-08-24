package com.vetsoftware.app.pricelist.domain;

/**
 * Ciclo de vida de una tarifa: {@code DRAFT} editable, {@code PUBLISHED}
 * congelada, {@code ARCHIVED} consultable.
 *
 * <p>
 * Subir precios no es editar una lista: es publicar una lista nueva. Por eso
 * solo {@link #DRAFT} admite cambios —lo impone
 * {@link PriceList#requireDraft()}, la regla R9 de
 * {@code suscripciones-reglas-codigo.md}— y las dos transiciones legales son
 * {@code DRAFT → PUBLISHED} y {@code PUBLISHED → ARCHIVED}.
 */
public enum PriceListStatus {
    DRAFT, PUBLISHED, ARCHIVED
}
