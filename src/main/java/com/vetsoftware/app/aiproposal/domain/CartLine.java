package com.vetsoftware.app.aiproposal.domain;

import com.vetsoftware.app.shared.domain.Money;
import java.math.BigDecimal;

/**
 * Una linea tal como la deja el motor determinista, antes de persistirse.
 *
 * <p>
 * Las lineas rechazadas <strong>tambien</strong> son {@code CartLine}: se
 * persisten con su {@code source} y su {@code verdict} porque son la senal con
 * la que se mide la calidad del modelo. Lo que no sale nunca por HTTP es el
 * veredicto ({@link LineVerdict}).
 *
 * <p>
 * <strong>El importe y el impuesto son por articulo.</strong> El 19 % no se
 * cablea en ningun sitio: cada linea lleva su {@code taxRate}, que viene de
 * {@code catalog_prices.tax_rate}, y hay articulos exentos.
 *
 * @param reason
 *            la prosa del modelo, ya saneada. Obligatoria para {@code MODEL} y
 *            {@code MODEL_RECOMMENDED} -espejo de
 *            {@code chk_ai_proposal_lines_model_reason}-
 */
public record CartLine(String code, String name, String shortDescription, SellableItemKind kind,
        LineSource source, LineVerdict verdict, int quantity, BigDecimal unitAmount,
        BigDecimal taxRate, int trialDays, String currency, String reason, int sortOrder) {

    public CartLine {
        if (code == null || code.isBlank())
            throw new IllegalArgumentException("line code is required");
        if (code.length() > 50)
            throw new IllegalArgumentException("line code must be 50 chars or less: " + code);
        if (source == null)
            throw new IllegalArgumentException("line source is required: " + code);
        if (verdict == null)
            throw new IllegalArgumentException("line verdict is required: " + code);
        if (quantity < 1)
            throw new IllegalArgumentException("line quantity must be at least 1: " + code);
        if (trialDays < 0)
            throw new IllegalArgumentException("line trialDays cannot be negative: " + code);
        if (reason != null && reason.length() > 500)
            throw new IllegalArgumentException("line reason must be 500 chars or less: " + code);
        if (sortOrder < 0)
            throw new IllegalArgumentException("line sortOrder cannot be negative: " + code);
        if (source.exigeMotivo() && (reason == null || reason.isBlank()))
            throw new IllegalArgumentException(
                    "a model line needs a written reason: " + code + " (" + source + ")");
        if (verdict.esAceptado() && (unitAmount == null || currency == null))
            throw new IllegalArgumentException(
                    "an accepted line needs price and currency: " + code);
    }

    /**
     * {@code unitAmount x quantity}, redondeado a centavos. Cero si no se cotiza.
     */
    public BigDecimal base() {
        if (!verdict.esAceptado() || unitAmount == null)
            return Money.zero();
        return Money.multiply(unitAmount, BigDecimal.valueOf(quantity));
    }

    /** El IVA de esta linea y solo de esta linea. */
    public BigDecimal impuesto() {
        return Money.percentOf(base(), taxRate);
    }

    public BigDecimal totalConImpuesto() {
        return base().add(impuesto());
    }

    /**
     * Una linea con prueba gratis no se cobra el primer periodo. Es la mitad de la
     * comparacion de paquete que la v1 no contaba (plan S1.5).
     */
    public boolean gratisElPrimerPeriodo() {
        return trialDays > 0;
    }
}
