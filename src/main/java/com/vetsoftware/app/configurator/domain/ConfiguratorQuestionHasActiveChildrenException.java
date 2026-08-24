package com.vetsoftware.app.configurator.domain;

/**
 * Borrar en lógico una pregunta u opción de la que todavía cuelga algo deja el
 * cuestionario con ramas huérfanas: una opción cuya pregunta ya no se muestra,
 * o un efecto que nadie puede disparar y que sigue metiendo artículos en el
 * carrito de quien llegue por otra rama.
 */
public class ConfiguratorQuestionHasActiveChildrenException extends RuntimeException {
    public ConfiguratorQuestionHasActiveChildrenException(String parent, Long id,
            String childType) {
        super("Cannot delete " + parent + " " + id + ": has active " + childType + " children");
    }
}
