package com.vetsoftware.app.shared.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Kernel compartido de aritmética monetaria (COP, 2 decimales, HALF_UP).
 *
 * <p>Es la ÚNICA excepción documentada al vertical slicing: un helper de dominio PURO (sin estado,
 * sin Spring, sin FKs a otra feature) que centraliza el redondeo a escala monetaria y el desglose de
 * la base gravable. Existe para que las features de cargo (general/producto/servicio) y la facturación
 * electrónica no dupliquen la matemática del dinero. No agregar aquí lógica de negocio de ninguna
 * feature concreta: solo aritmética monetaria genérica.
 */
public final class Money {
    /** Escala monetaria: 2 decimales (centavos COP). */
    public static final int SCALE = 2;
    /** Redondeo monetario: HALF_UP (redondeo comercial). */
    public static final RoundingMode ROUND = RoundingMode.HALF_UP;

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final BigDecimal THOUSAND = BigDecimal.valueOf(1000);
    /** Escala intermedia del factor (1 + tasa/100) antes de dividir el total. */
    private static final int RATE_SCALE = 6;

    private Money() {}

    /** Redondea a escala monetaria (2 dec, HALF_UP). Null-safe: {@code null → null}. */
    public static BigDecimal scaled(BigDecimal value) {
        return value == null ? null : value.setScale(SCALE, ROUND);
    }

    /** Cero con escala monetaria. */
    public static BigDecimal zero() {
        return BigDecimal.ZERO.setScale(SCALE);
    }

    /** Producto a escala monetaria: {@code a · b} (2 dec, HALF_UP). */
    public static BigDecimal multiply(BigDecimal a, BigDecimal b) {
        return a.multiply(b).setScale(SCALE, ROUND);
    }

    /**
     * Extrae la base gravable de un total CON impuesto incluido: {@code base = total / (1 + pct/100)}.
     * {@code pct} null o 0 → el total ya es la base (sin impuesto).
     */
    public static BigDecimal extractBase(BigDecimal total, BigDecimal percentage) {
        if (percentage == null || percentage.signum() == 0) return total;
        BigDecimal factor = BigDecimal.ONE.add(percentage.divide(HUNDRED, RATE_SCALE, ROUND));
        return total.divide(factor, SCALE, ROUND);
    }

    /**
     * Porcentaje sobre un monto: {@code amount · rate / 100} (2 dec, HALF_UP).
     * Devuelve 0 si {@code amount}/{@code rate} son null o {@code rate ≤ 0}.
     */
    public static BigDecimal percentOf(BigDecimal amount, BigDecimal rate) {
        if (amount == null || rate == null || rate.signum() <= 0) return BigDecimal.ZERO;
        return amount.multiply(rate).divide(HUNDRED, SCALE, ROUND);
    }

    /**
     * Por mil sobre un monto: {@code amount · rate / 1000} (2 dec, HALF_UP). Para tarifas expresadas en ‰
     * (p. ej. la retención de ICA: 9,66 ‰ = 0,966 %). Devuelve 0 si {@code amount}/{@code rate} son null o
     * {@code rate ≤ 0}.
     */
    public static BigDecimal perMilOf(BigDecimal amount, BigDecimal rate) {
        if (amount == null || rate == null || rate.signum() <= 0) return BigDecimal.ZERO;
        return amount.multiply(rate).divide(THOUSAND, SCALE, ROUND);
    }
}
