package com.vetsoftware.app.configurator.application.usecase;

import com.vetsoftware.app.configurator.application.port.out.ConfiguratorOptionRepository;
import com.vetsoftware.app.configurator.application.port.out.ConfiguratorQuestionRepository;
import com.vetsoftware.app.configurator.domain.ConfiguratorOptionNotFoundException;
import com.vetsoftware.app.configurator.domain.ConfiguratorQuestionTree;

/**
 * Carga el árbol y le pregunta al dominio si el arco nuevo cierra un ciclo.
 *
 * <p>
 * Vive fuera de los dos servicios que lo usan —alta y edición de pregunta—
 * porque escribir la comprobación dos veces es cómo una de las dos se queda sin
 * ella: la edición es justamente el camino por el que un ciclo entra, y es el
 * que más fácil se olvida.
 *
 * <p>
 * Estático y con los puertos por parámetro a propósito: no es un caso de uso y
 * no debe ser un bean más que alguien pueda inyectar por error donde toca un
 * {@code UseCase}.
 */
final class ConditionalQuestionGuard {

    private ConditionalQuestionGuard() {
    }

    /**
     * @param questionId
     *            la pregunta que se guarda, o {@code null} si es nueva
     * @param parentOptionId
     *            la opción de la que va a colgar, o {@code null} para dejarla en la
     *            raíz
     */
    static void assertParentIsUsable(Long questionId, Long parentOptionId,
            ConfiguratorQuestionRepository questions, ConfiguratorOptionRepository options) {
        if (parentOptionId == null) {
            return;
        }
        // La existencia se comprueba aquí y no en el recorrido: el arbol solo sabe de
        // topologia, y «la opcion no existe» es un 404 con nombre, no un ciclo.
        options.findById(parentOptionId)
                .orElseThrow(() -> new ConfiguratorOptionNotFoundException(parentOptionId));
        ConfiguratorQuestionTree.assertNoCycle(questionId, parentOptionId,
                ConfiguratorQuestionTree.questionIdByOptionId(options.findAllOrdered()),
                ConfiguratorQuestionTree.parentOptionIdByQuestionId(questions.findAllOrdered()));
    }
}
