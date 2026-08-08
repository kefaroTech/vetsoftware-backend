package com.vetsoftware.app.auth.application.dto;

import java.util.Set;

/**
 * Actor autenticado de la request. Jerarquía cerrada de tres implementadores.
 *
 * <p>
 * Consúmela siempre con un {@code switch} exhaustivo <strong>sin
 * {@code default}</strong>: así, añadir un cuarto actor a {@code permits} rompe
 * la compilación en cada punto de decisión en lugar de caer en silencio en un
 * caso no contemplado. Las cadenas de {@code if/else-if instanceof} tiran ese
 * chequeo del compilador.
 */
public sealed interface AuthContext permits EmployeeContext, SystemUserContext, SystemContext {

    Set<String> permissions();

    /**
     * Estrecha el principal de Spring Security al tipo sellado. Es el único punto
     * de conversión del sistema: {@code Authentication.getPrincipal()} devuelve
     * {@code Object}, y un {@code switch} sobre {@code Object} exigiría un
     * {@code default} — perdiendo justo la exhaustividad que buscamos. A partir de
     * aquí el selector es {@code AuthContext} y el compilador la verifica.
     *
     * @param principal
     *            el principal de la {@code Authentication}, admite {@code null}
     * @return el actor, o {@code null} si no hay uno autenticado; trátalo con
     *         {@code case null} en el switch
     */
    static AuthContext ofPrincipal(Object principal) {
        return principal instanceof AuthContext context ? context : null;
    }
}
