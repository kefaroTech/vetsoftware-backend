package com.vetsoftware.app.configurator.domain;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * El árbol de preguntas condicionales, y el único sitio donde se comprueba que
 * lo sea.
 *
 * <p>
 * Cada pregunta puede colgar de una opción ({@code parent_option_id}) y cada
 * opción pertenece a una pregunta ({@code question_id}). Encadenando las dos
 * relaciones se sube de una pregunta a su ascendiente. La base garantiza que
 * cada arco apunta a una fila existente; <strong>no</strong> garantiza que la
 * subida termine, porque un {@code CHECK} de MySQL no recorre filas.
 *
 * <p>
 * Un ciclo no da un error: da un cuestionario sin raíz. El asistente no
 * encuentra por dónde empezar y el prospecto —que todavía no es cliente y no
 * tiene a quién llamar— se va. Por eso se comprueba al guardar, que es el único
 * instante en que el arco nuevo aún no existe y la ascendencia ya es conocida.
 *
 * <p>
 * Clase de estáticos y sin estado: recibe el mapa entero del cuestionario, que
 * son decenas de filas. No hay ninguna razón para recorrer la base arco a arco.
 */
public final class ConfiguratorQuestionTree {

    private ConfiguratorQuestionTree() {
    }

    /**
     * Comprueba que colgar {@code questionId} de {@code parentOptionId} no cierre
     * un ciclo.
     *
     * @param questionId
     *            la pregunta que se está guardando; {@code null} si todavía no
     *            existe — una pregunta nueva no puede cerrar un ciclo porque nadie
     *            la apunta aún, pero su ascendencia sí puede estar ya podrida y
     *            entonces también se rechaza
     * @param parentOptionId
     *            la opción de la que va a colgar; {@code null} deja la pregunta en
     *            la raíz y no hay nada que comprobar
     * @param questionIdByOptionId
     *            de cada opción, la pregunta a la que pertenece
     * @param parentOptionIdByQuestionId
     *            de cada pregunta condicional, la opción de la que cuelga
     * @throws ConditionalQuestionCycleException
     *             si la subida vuelve a {@code questionId} o si se repite un nodo,
     *             que es un ciclo preexistente en los datos
     */
    public static void assertNoCycle(Long questionId, Long parentOptionId,
            Map<Long, Long> questionIdByOptionId, Map<Long, Long> parentOptionIdByQuestionId) {
        if (parentOptionId == null) {
            return;
        }
        Set<Long> visitadas = new HashSet<>();
        Long optionId = parentOptionId;
        while (optionId != null) {
            Long ancestro = questionIdByOptionId.get(optionId);
            if (ancestro == null) {
                // La opción no existe o está dada de baja: no hay más ascendencia que
                // recorrer. Que exista es trabajo del caso de uso, no de esta subida.
                return;
            }
            if (Objects.equals(ancestro, questionId)) {
                throw new ConditionalQuestionCycleException("Conditional question cycle: question "
                        + questionId + " cannot depend on option " + parentOptionId
                        + ", which already descends from it");
            }
            if (!visitadas.add(ancestro)) {
                throw new ConditionalQuestionCycleException(
                        "Conditional question cycle already present above option " + parentOptionId
                                + ": question " + ancestro + " is its own ancestor");
            }
            optionId = parentOptionIdByQuestionId.get(ancestro);
        }
    }

    /** De cada opción, la pregunta a la que pertenece. */
    public static Map<Long, Long> questionIdByOptionId(List<ConfiguratorOption> options) {
        return options.stream().filter(option -> option.getId() != null).collect(Collectors.toMap(
                ConfiguratorOption::getId, ConfiguratorOption::getQuestionId, (uno, otro) -> uno));
    }

    /**
     * De cada pregunta condicional, la opción de la que cuelga. Las preguntas de
     * raíz se omiten a propósito: un {@code null} en el mapa y una ausencia
     * significan lo mismo para la subida, y omitirlas hace el mapa más pequeño.
     */
    public static Map<Long, Long> parentOptionIdByQuestionId(List<ConfiguratorQuestion> questions) {
        return questions.stream()
                .filter(question -> question.getId() != null && question.isConditional())
                .collect(Collectors.toMap(ConfiguratorQuestion::getId,
                        ConfiguratorQuestion::getParentOptionId, (uno, otro) -> uno));
    }
}
