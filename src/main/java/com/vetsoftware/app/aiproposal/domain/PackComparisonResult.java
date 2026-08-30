package com.vetsoftware.app.aiproposal.domain;

import java.math.BigDecimal;
import java.util.List;

/**
 * La oferta de paquete, con <strong>las dos dimensiones</strong>: lo que se
 * ahorra al mes y lo que cuesta en dias de prueba.
 *
 * <p>
 * Nunca representa una sustitucion. Es una oferta que el cliente acepta con un
 * clic, y el carrito por defecto siguen siendo los modulos sueltos -los que
 * conservan la prueba-. Sustituir en silencio ahorraba 35.000 al mes y le
 * quitaba al cliente ~164.500 del primer mes, con toda la landing prometiendo
 * "prueba gratis, sin tarjeta" (plan S1.5).
 *
 * @param sumaSuelta
 *            lo que cuestan hoy, sueltos, los modulos que el paquete cubre
 *            -antes de impuestos, que es como se comparan los precios de lista-
 * @param diasDePruebaPerdidos
 *            el mayor numero de dias que se pierde en alguna linea; es el
 *            titular que lee el cliente
 * @param modulosQuePierdenPrueba
 *            los nombres, para poder decir cuales, en vez de un numero suelto
 *            que no se puede verificar
 */
public record PackComparisonResult(String packCode, String packName, BigDecimal packAmount,
        BigDecimal sumaSuelta, BigDecimal ahorroMensual, String currency, int diasDePruebaPerdidos,
        List<String> modulosQuePierdenPrueba) {

    public PackComparisonResult {
        if (packCode == null || packCode.isBlank())
            throw new IllegalArgumentException("packCode is required");
        if (packName == null || packName.isBlank())
            throw new IllegalArgumentException("packName is required: " + packCode);
        if (packAmount == null || sumaSuelta == null || ahorroMensual == null)
            throw new IllegalArgumentException("pack comparison needs its three amounts");
        if (ahorroMensual.signum() <= 0)
            throw new IllegalArgumentException(
                    "a pack that does not save money is not an offer: " + packCode);
        if (currency == null || currency.length() != 3)
            throw new IllegalArgumentException("pack comparison currency is required: " + packCode);
        if (diasDePruebaPerdidos < 0)
            throw new IllegalArgumentException("trial days lost cannot be negative: " + packCode);
        modulosQuePierdenPrueba = List
                .copyOf(modulosQuePierdenPrueba == null ? List.of() : modulosQuePierdenPrueba);
    }

    /**
     * Si aceptarlo no cuesta ni un dia de prueba, la oferta se puede presentar sin
     * la advertencia. Con los tres paquetes {@code NEVER_FREE} de hoy esto es
     * {@code false} casi siempre, y por eso la advertencia existe.
     */
    public boolean sinCosteEnPrueba() {
        return diasDePruebaPerdidos == 0;
    }
}
