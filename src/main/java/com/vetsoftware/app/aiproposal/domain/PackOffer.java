package com.vetsoftware.app.aiproposal.domain;

import java.math.BigDecimal;
import java.util.Set;

/**
 * Un paquete con la lista de <strong>modulos</strong> que trae dentro.
 *
 * <p>
 * <strong>Solo los componentes de tipo {@code MODULE}</strong>, y esa es la
 * correccion que convirtio la comparacion en codigo vivo (plan S1.5).
 * {@code CAPACITY_TERMINAL} es componente de los tres paquetes y nunca entra al
 * carrito por si solo, asi que "el paquete esta contenido en el carrito" no se
 * cumplia <em>nunca</em>: la funcion estrella de la v1 era codigo muerto. Los
 * {@code CAPACITY} del paquete se tratan como concedidos y no participan en la
 * contencion.
 *
 * @param trialDays
 *            los tres paquetes son {@code NEVER_FREE} y aqui valen cero; el
 *            campo existe en general porque la formula del coste en prueba
 *            resta, no asume cero
 */
public record PackOffer(String code, String name, BigDecimal unitAmount, BigDecimal taxRate,
        int trialDays, Set<String> moduleComponentCodes) {

    public PackOffer {
        if (code == null || code.isBlank())
            throw new IllegalArgumentException("pack code is required");
        if (name == null || name.isBlank())
            throw new IllegalArgumentException("pack name is required: " + code);
        if (unitAmount == null || unitAmount.signum() < 0)
            throw new IllegalArgumentException("pack unitAmount must be zero or positive: " + code);
        if (taxRate == null || taxRate.signum() < 0)
            throw new IllegalArgumentException("pack taxRate must be zero or positive: " + code);
        if (trialDays < 0)
            throw new IllegalArgumentException("pack trialDays cannot be negative: " + code);
        if (moduleComponentCodes == null)
            throw new IllegalArgumentException("pack module components are required: " + code);
        moduleComponentCodes = Set.copyOf(moduleComponentCodes);
    }

    /**
     * Un paquete sin ni un modulo dentro no se puede comparar con nada: la
     * contencion de un conjunto vacio es cierta por vacuidad y la oferta saldria
     * <em>siempre</em>, con cualquier carrito, incluso con uno vacio. Es el mismo
     * defecto que hacia muerta a la regla de la v1, con el signo cambiado.
     */
    public boolean esComparable() {
        return !moduleComponentCodes.isEmpty();
    }
}
