package com.vetsoftware.app.customercredit.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Un lote de saldo vivo: el asiento de alta ({@code GRANT}) junto con lo que le
 * queda despues de restarle sus consumos y su caducidad.
 *
 * <p>
 * <strong>No es una fila de la base</strong>, es el resultado de netear el
 * libro por lote. Existe porque el consumo <em>tiene</em> que saber de que lote
 * sale: sin lotes la caducidad no es calculable y la suma admite dos respuestas
 * defendibles (D-71).
 *
 * <p>
 * El orden en que llegan estos lotes es parte del contrato del puerto que los
 * devuelve, no una decision del consumidor: <strong>primero el que antes
 * caduca</strong>, los sin fecha al final, y desempate por {@code entryId}.
 *
 * @param entryId
 *            el {@code GRANT} que abrio el lote; es lo que el consumo anota en
 *            {@code lot_entry_id}
 * @param remaining
 *            remanente vivo, siempre positivo: un lote agotado no es un lote
 * @param expiresOn
 *            cuando caduca lo que queda, o {@code null} si no caduca
 */
public record CreditLot(Long entryId, BigDecimal remaining, LocalDate expiresOn) {

    public CreditLot {
        if (entryId == null)
            throw new IllegalArgumentException("credit lot entry id is required");
        if (remaining == null)
            throw new IllegalArgumentException("credit lot remaining is required");
        if (remaining.signum() <= 0)
            throw new IllegalArgumentException("credit lot remaining must be greater than zero");
    }

    /** Lo que se puede sacar de este lote sin pasarse de lo que se pide. */
    public BigDecimal take(BigDecimal wanted) {
        return wanted.min(remaining);
    }
}
