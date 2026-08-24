package com.vetsoftware.app.quote.domain;

import java.time.LocalDate;

/**
 * La cotizacion ya vencio: no se puede enviar ni aceptar.
 *
 * <p>
 * Es la razon de ser de valid_until. Sin el, alguien aparece en 2029 con una
 * cotizacion de 2026 y tiene razon.
 */
public class QuoteExpiredException extends IllegalStateException {
    public QuoteExpiredException(Long id, LocalDate validUntil) {
        super("Quote " + id + " expired on " + validUntil);
    }
}
