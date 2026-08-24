package com.vetsoftware.app.configurator.domain;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Comprueba que un conjunto de respuestas encaje en el árbol del cuestionario,
 * <strong>antes</strong> de traducirlo en carrito.
 *
 * <p>
 * {@link ConfiguratorQuestionTree} vigila la topología al guardar: que las
 * preguntas condicionales no formen un ciclo. Eso no dice nada sobre las
 * respuestas. Sin esta clase, {@code parent_option_id} es decorativo en el
 * único momento en que importa: al resolver, una respuesta a una pregunta que
 * el asistente nunca mostró dispara sus efectos igual que cualquier otra.
 *
 * <p>
 * <strong>Por qué es cara de omitir.</strong> La cadena que sigue es corta y
 * toda automática: la selección se convierte en líneas de cotización con precio
 * congelado, la cotización aceptada se convierte en contrato y el contrato
 * devenga cargos. Un artículo colado en el primer eslabón acaba en una cuenta
 * de cobro con número consecutivo, y ahí ya no se corrige editando: hay que
 * emitir una nota crédito.
 *
 * <p>
 * <strong>Las tres mitades.</strong> Cerrar solo la adición dejaría la puerta
 * de al lado abierta, así que se comprueban todas:
 * <ul>
 * <li><em>Nada sobra</em>: toda respuesta pertenece a una pregunta alcanzable
 * con las respuestas dadas, recursivamente hasta la raíz.</li>
 * <li><em>Nada falta</em>: toda pregunta obligatoria cuya rama sí está activa
 * viene respondida.</li>
 * <li><em>Nada viene en la forma equivocada</em>: la respuesta es del tipo que
 * la pregunta admite, y en la cantidad que admite.</li>
 * </ul>
 *
 * <p>
 * <strong>Por qué el tipo se comprueba aquí y no en un guardián
 * aparte.</strong> Encajar en el cuestionario y encajar con el tipo de la
 * pregunta son la misma invariante mirada desde dos ángulos: un número mandado
 * a una pregunta {@code SINGLE} es tan huérfano como una respuesta de una rama
 * que nadie activó, porque el asistente nunca pintó ese campo. Separarlas en
 * dos clases garantiza que un día una se actualice y la otra no — y la que se
 * quede atrás es la que deja pasar la cotización.
 *
 * <p>
 * <strong>La cardinalidad no es un detalle cosmético: sin ella la
 * alcanzabilidad es evitable.</strong> Una pregunta {@code SINGLE} activa una
 * rama distinta por cada opción. Si se admiten dos opciones marcadas de la
 * misma pregunta, basta con marcar <em>todas</em> las opciones del cuestionario
 * para que toda rama sea alcanzable y todo efecto dispare: la comprobación de
 * arriba seguiría pasando y el carrito saldría con el catálogo entero. Solo
 * {@code MULTI} admite varias, que es exactamente lo que significa.
 *
 * <p>
 * Puro, como {@link ConfiguratorQuestionTree}: recibe el cuestionario ya
 * cargado y no conoce Spring, repositorios ni reloj.
 */
public final class ConfiguratorAnswerCoherence {

    private ConfiguratorAnswerCoherence() {
    }

    /**
     * @param questions
     *            las preguntas activas del cuestionario
     * @param options
     *            las opciones activas del cuestionario
     * @param answers
     *            lo que respondió el prospecto
     * @throws UnreachableAnswerException
     *             si alguna respuesta cuelga de una rama que no está activa, señala
     *             una fila que no existe, o viene en una forma que su pregunta no
     *             admite —un número para una pregunta que no es {@code NUMBER}, una
     *             opción de una pregunta que sí lo es, o una segunda opción marcada
     *             en una pregunta que solo admite una
     * @throws MissingRequiredAnswerException
     *             si una pregunta obligatoria de una rama activa no viene
     *             respondida
     * @throws ConditionalQuestionCycleException
     *             si la ascendencia de una pregunta es cíclica; no debería poder
     *             ocurrir tras la validación de guardado, pero un ciclo en los
     *             datos colgaría el recorrido y hay que nombrarlo
     */
    public static void assertCoherent(List<ConfiguratorQuestion> questions,
            List<ConfiguratorOption> options, ConfiguratorAnswers answers) {
        Map<Long, ConfiguratorQuestion> questionById = questions.stream()
                .filter(question -> question.getId() != null).collect(Collectors.toMap(
                        ConfiguratorQuestion::getId, Function.identity(), (uno, otro) -> uno));
        Map<Long, ConfiguratorOption> optionById = options.stream()
                .filter(option -> option.getId() != null).collect(Collectors
                        .toMap(ConfiguratorOption::getId, Function.identity(), (uno, otro) -> uno));

        assertNothingIsUnknown(answers, questionById.keySet(), optionById.keySet());
        // El tipo se comprueba antes que la alcanzabilidad a propósito: es la
        // comprobación más específica de las tres —«esta pregunta no se responde
        // así»— y no recorre el árbol, así que cuando una respuesta manipulada
        // incumple las dos, el mensaje que sale es el accionable.
        assertTypesFit(questions, options, answers, questionById);
        assertNothingExtra(questions, options, answers, questionById, optionById);
        assertNothingMissing(questions, options, answers, questionById, optionById);
    }

    /**
     * Un id que no existe se nombra aparte de una rama no activada: son dos
     * diagnósticos distintos —un front desincronizado contra un cuestionario
     * reeditado, frente a un intento de manipulación— y confundirlos manda a
     * investigar al sitio equivocado.
     *
     * <p>
     * Los ids se ordenan para que el mensaje sea el mismo en dos ejecuciones
     * iguales: el {@code Set} inmutable de {@link ConfiguratorAnswers} no promete
     * orden de iteración.
     */
    private static void assertNothingIsUnknown(ConfiguratorAnswers answers,
            Set<Long> preguntasConocidas, Set<Long> opcionesConocidas) {
        Set<Long> opcionesRaras = new TreeSet<>(answers.selectedOptionIds());
        opcionesRaras.removeAll(opcionesConocidas);
        if (!opcionesRaras.isEmpty()) {
            Long opcion = opcionesRaras.iterator().next();
            // Sin questionCode a proposito: la opcion no existe, asi que no hay pregunta de
            // la que colgarla. Inventarse una seria peor que no darla.
            throw new UnreachableAnswerException(
                    "Answer refers to option " + opcion
                            + ", which does not exist in the questionnaire or is no longer active",
                    null, null, opcion);
        }
        Set<Long> preguntasRaras = new TreeSet<>(answers.numericAnswers().keySet());
        preguntasRaras.removeAll(preguntasConocidas);
        if (!preguntasRaras.isEmpty()) {
            Long pregunta = preguntasRaras.iterator().next();
            throw new UnreachableAnswerException(
                    "Answer refers to question " + pregunta
                            + ", which does not exist in the questionnaire or is no longer active",
                    pregunta, null, null);
        }
    }

    /**
     * Nada viene en la forma equivocada. La tabla es corta y no tiene casos grises:
     * <ul>
     * <li>{@code NUMBER}: admite un número y <strong>ninguna opción</strong>. Que
     * cuelguen filas de opción de una pregunta numérica es un cuestionario mal
     * configurado, y marcarlas dispararía sus efectos igual que las de cualquier
     * otra pregunta.</li>
     * <li>{@code SINGLE} y {@code BOOLEAN}: admiten <strong>como mucho una</strong>
     * opción y ningún número. {@code BOOLEAN} es «sí o no» modelado como dos
     * opciones, así que marcar las dos es tan imposible en el asistente como marcar
     * dos radios de un {@code SINGLE}.</li>
     * <li>{@code MULTI}: admite varias opciones y ningún número. Es el único tipo
     * al que la cardinalidad no le dice nada.</li>
     * </ul>
     *
     * <p>
     * Que sea «como mucho una» y no «exactamente una» es deliberado: exigir que
     * haya respuesta es trabajo de {@link #assertNothingMissing}, que ya sabe
     * distinguir una pregunta obligatoria de una que no lo es. Duplicar aquí ese
     * criterio convertiría toda pregunta opcional en obligatoria.
     *
     * <p>
     * <strong>Se rechaza, no se descarta</strong>, que es el criterio de las otras
     * dos comprobaciones y por la misma razón: descartar en silencio el número que
     * sobra produce un carrito distinto del que pidió el cliente y no deja rastro
     * ni del intento ni del front que lo provocó. Desde que
     * {@code POST /configurator/resolve} es anónimo, el cuerpo entero lo controla
     * quien llama, sin cuenta con la que responder después.
     */
    private static void assertTypesFit(List<ConfiguratorQuestion> questions,
            List<ConfiguratorOption> options, ConfiguratorAnswers answers,
            Map<Long, ConfiguratorQuestion> questionById) {
        for (ConfiguratorQuestion question : questions) {
            if (question.getId() == null || !answers.numericAnswers().containsKey(question.getId())
                    || question.getAnswerType() == AnswerType.NUMBER) {
                continue;
            }
            throw noEncaja(question, "the questionnaire never showed a numeric field for it");
        }
        Map<Long, Long> primeraMarcada = new HashMap<>();
        for (ConfiguratorOption option : options) {
            if (option.getId() == null || !answers.selectedOptionIds().contains(option.getId())) {
                continue;
            }
            ConfiguratorQuestion pregunta = questionById.get(option.getQuestionId());
            if (pregunta == null) {
                // La pregunta dueña está dada de baja: la rama está rota, y eso lo
                // nombra assertNothingExtra, que sabe decir dónde se rompió.
                continue;
            }
            if (pregunta.getAnswerType() == AnswerType.NUMBER) {
                throw noEncaja(pregunta, "it is answered with a number, but option "
                        + option.getId() + " was selected");
            }
            Long primera = primeraMarcada.putIfAbsent(pregunta.getId(), option.getId());
            if (primera != null && pregunta.getAnswerType() != AnswerType.MULTI) {
                throw noEncaja(pregunta, "it admits a single answer, but options " + primera
                        + " and " + option.getId() + " were both selected");
            }
        }
    }

    /**
     * Es {@link UnreachableAnswerException} a propósito, y no una excepción nueva.
     * Para quien llama es exactamente el mismo suceso que una respuesta huérfana
     * —el asistente nunca mostró eso, el envío se corrige igual y el estado
     * guardado no tiene nada que ver—, así que se mapea al mismo 400
     * {@code CONFIGURATOR_ANSWER_UNREACHABLE} y el front trata una sola cosa. Dos
     * códigos de error para una única invariante le obligarían a escribir dos veces
     * el mismo tratamiento, y a acordarse del segundo.
     *
     * <p>
     * Tampoco es {@link QuantityFromAnswerRequiresNumberQuestionException}, aunque
     * hable del mismo desajuste de tipos: aquella es un 409 dirigido a quien
     * <em>edita</em> el cuestionario y su propia decisión escrita es que no se
     * lanza al cotizar, porque convierte un error de configuración de hace meses en
     * un fallo en la cara de un cliente que no puede arreglarlo. Aquí el culpable
     * es el cuerpo que acaba de llegar.
     */
    private static UnreachableAnswerException noEncaja(ConfiguratorQuestion question,
            String porque) {
        return new UnreachableAnswerException("Answer to question " + question.getId() + " ("
                + question.getCode() + ") does not fit its answer type " + question.getAnswerType()
                + ": " + porque, question.getId(), question.getCode(), null);
    }

    /**
     * Nada sobra. Se recorre en el orden del cuestionario —no en el del conjunto de
     * respuestas— para que, con varias respuestas huérfanas, el error señale
     * siempre la misma y el mensaje sea reproducible.
     */
    private static void assertNothingExtra(List<ConfiguratorQuestion> questions,
            List<ConfiguratorOption> options, ConfiguratorAnswers answers,
            Map<Long, ConfiguratorQuestion> questionById,
            Map<Long, ConfiguratorOption> optionById) {
        for (ConfiguratorOption option : options) {
            if (!answers.selectedOptionIds().contains(option.getId())) {
                continue;
            }
            Alcance alcance = alcanceDe(option.getQuestionId(), questionById, optionById,
                    answers.selectedOptionIds());
            if (!alcance.alcanzable()) {
                throw unreachable(questionById.get(option.getQuestionId()), option.getQuestionId(),
                        alcance.opcionQueBloquea());
            }
        }
        for (ConfiguratorQuestion question : questions) {
            if (!answers.numericAnswers().containsKey(question.getId())) {
                continue;
            }
            Alcance alcance = alcanceDe(question.getId(), questionById, optionById,
                    answers.selectedOptionIds());
            if (!alcance.alcanzable()) {
                throw unreachable(question, question.getId(), alcance.opcionQueBloquea());
            }
        }
    }

    /** Nada falta: la mitad que cierra la manipulación por omisión. */
    private static void assertNothingMissing(List<ConfiguratorQuestion> questions,
            List<ConfiguratorOption> options, ConfiguratorAnswers answers,
            Map<Long, ConfiguratorQuestion> questionById,
            Map<Long, ConfiguratorOption> optionById) {
        Map<Long, List<ConfiguratorOption>> porPregunta = options.stream()
                .collect(Collectors.groupingBy(ConfiguratorOption::getQuestionId));
        for (ConfiguratorQuestion question : questions) {
            if (!question.isRequired() || question.getId() == null) {
                continue;
            }
            boolean activa = alcanceDe(question.getId(), questionById, optionById,
                    answers.selectedOptionIds()).alcanzable();
            if (!activa || estaRespondida(question, porPregunta, answers)) {
                continue;
            }
            throw new MissingRequiredAnswerException(question.getId(), question.getCode());
        }
    }

    /**
     * Una pregunta numérica se responde con un número; las demás, marcando alguna
     * de sus opciones. Una pregunta obligatoria y sin opciones que no sea
     * {@code NUMBER} es imposible de responder, y por eso cuenta como no respondida
     * en vez de darse por buena: es un cuestionario mal configurado, y darlo por
     * bueno lo dejaría cotizando sin que nadie lo vea.
     */
    private static boolean estaRespondida(ConfiguratorQuestion question,
            Map<Long, List<ConfiguratorOption>> porPregunta, ConfiguratorAnswers answers) {
        if (question.getAnswerType() == AnswerType.NUMBER) {
            return answers.numericAnswers().containsKey(question.getId());
        }
        return porPregunta.getOrDefault(question.getId(), List.of()).stream()
                .anyMatch(option -> answers.selectedOptionIds().contains(option.getId()));
    }

    /**
     * Si una pregunta es alcanzable y, cuando no lo es, dónde se rompió la cadena.
     *
     * <p>
     * Es un tipo y no un {@code Optional<Long>} porque hay un caso en que la
     * pregunta está bloqueada y <strong>no hay ninguna opción que nombrar</strong>:
     * cuando la propia pregunta dueña de la respuesta está dada de baja y no se
     * llegó a seguir ningún padre. Con {@code Optional}, ese caso devolvía vacío
     * —es decir, «alcanzable»— y dejaba pasar exactamente lo que esta clase existe
     * para parar.
     *
     * @param alcanzable
     *            si la pregunta se muestra con esas respuestas
     * @param opcionQueBloquea
     *            la opción de la ascendencia que no está marcada; {@code null} si
     *            la rama está rota por una fila dada de baja
     */
    private record Alcance(boolean alcanzable, Long opcionQueBloquea) {

        static Alcance si() {
            return new Alcance(true, null);
        }

        static Alcance no(Long opcionQueBloquea) {
            return new Alcance(false, opcionQueBloquea);
        }
    }

    /**
     * Sube de la pregunta a su ascendiente encadenando
     * {@code parent_option_id → question_id}, hasta la raíz.
     *
     * <p>
     * La opción concreta que bloquea es lo que hace el mensaje accionable: nombra
     * dónde se rompió la cadena, no solo que se rompió.
     */
    private static Alcance alcanceDe(Long questionId, Map<Long, ConfiguratorQuestion> questionById,
            Map<Long, ConfiguratorOption> optionById, Set<Long> marcadas) {
        Set<Long> visitadas = new HashSet<>();
        Long actual = questionId;
        Long ultimoPadreSeguido = null;
        while (actual != null) {
            if (!visitadas.add(actual)) {
                throw new ConditionalQuestionCycleException(
                        "Conditional question cycle reached while checking answers: question "
                                + actual + " is its own ancestor");
            }
            ConfiguratorQuestion pregunta = questionById.get(actual);
            if (pregunta == null) {
                // Un ancestro dado de baja parte la rama: lo que colgaba de el ya no se
                // muestra, asi que responderlo es tan huerfano como no haberlo activado.
                return Alcance.no(ultimoPadreSeguido);
            }
            Long padre = pregunta.getParentOptionId();
            if (padre == null) {
                return Alcance.si();
            }
            if (!marcadas.contains(padre)) {
                return Alcance.no(padre);
            }
            ConfiguratorOption opcionPadre = optionById.get(padre);
            if (opcionPadre == null) {
                return Alcance.no(padre);
            }
            ultimoPadreSeguido = padre;
            actual = opcionPadre.getQuestionId();
        }
        return Alcance.no(ultimoPadreSeguido);
    }

    private static UnreachableAnswerException unreachable(ConfiguratorQuestion question,
            Long questionId, Long parentOptionId) {
        String codigo = question == null ? "?" : question.getCode();
        String porque = parentOptionId == null
                ? "its branch is broken: an ancestor question is no longer active"
                : "it depends on option " + parentOptionId + ", which was not selected";
        return new UnreachableAnswerException(
                "Answer to question " + questionId + " (" + codigo + ") is not reachable: "
                        + porque,
                questionId, question == null ? null : question.getCode(), parentOptionId);
    }
}
