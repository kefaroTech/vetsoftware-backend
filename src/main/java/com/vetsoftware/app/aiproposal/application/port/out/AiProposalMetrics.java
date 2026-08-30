package com.vetsoftware.app.aiproposal.application.port.out;

import com.vetsoftware.app.aiproposal.domain.CartLine;
import com.vetsoftware.app.aiproposal.domain.CartResult;
import com.vetsoftware.app.aiproposal.domain.GenerationOutcome;
import com.vetsoftware.app.aiproposal.domain.LineVerdict;
import com.vetsoftware.app.aiproposal.domain.ProposalDraft;
import com.vetsoftware.app.aiproposal.domain.ProposalPresentation;
import com.vetsoftware.app.aiproposal.domain.ReasonRejection;
import com.vetsoftware.app.aiproposal.domain.SanitizedReason;
import java.util.List;
import java.util.Objects;

/**
 * Lo que el asistente publica cada vez que sirve una propuesta.
 *
 * <p>
 * <strong>Un solo metodo y un solo hecho.</strong> Todo lo que hay que contar
 * de un turno -que se sirvio, por que camino, como se pinta, que motivos
 * rechazo el saneador y que codigos no existian- viaja en
 * {@link ServedProposal} y se emite en una sola llamada, desde el caso de uso y
 * <strong>despues</strong> de que {@code ProposalTurnWriter} haya cerrado su
 * transaccion. Dos emisores para el mismo hecho es como se llega a que la traza
 * diga una cosa y la metrica otra, y publicar antes del commit es como se
 * cuentan propuestas que despues hacen rollback.
 *
 * <p>
 * &#9940; <strong>R1 vive tambien aqui.</strong> Ni un solo campo de este
 * contrato admite prosa: son enums de vocabulario cerrado, cuentas enteras y el
 * {@code BIGINT} de la propuesta. Ni el texto del prospecto, ni la prosa del
 * modelo, ni el {@code public_token} de 43 caracteres tienen sitio donde
 * entrar, y eso es por construccion y no por disciplina. El unico dato
 * <em>sobre</em> el texto que se emite es su longitud
 * ({@link ServedProposal#inputChars()}), que es exactamente lo que el anexo B
 * autoriza.
 *
 * <p>
 * <strong>Por que no hay metodo para el gasto.</strong> El coste lo publica
 * {@code InProcessDailySpendGuard}, que es el unico punto que ve <em>todos</em>
 * los cargos -tambien el del intento que fallo despues de pagar-. Emitirlo
 * desde el caso de uso dejaria fuera precisamente ese, y el contador diria
 * menos de lo que la factura de AWS.
 */
public interface AiProposalMetrics {

    /**
     * @param served
     *            el turno entero. Nunca {@code null}
     */
    void proposalServed(ServedProposal served);

    /** Cual de los dos endpoints publicos produjo el turno. */
    enum Operation {

        PROPOSE("propose"), REFINE("refine");

        private final String value;

        Operation(String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }
    }

    /**
     * Por que camino salio la propuesta, como <strong>vocabulario propio del slice
     * de telemetria</strong> y no como el enum de dominio.
     *
     * <p>
     * &#9940; La diferencia no es cosmetica: {@link GenerationOutcome} solo
     * describe lo que hizo el generador, y hay un desenlace que el generador no
     * llega a ver nunca -{@link #NO_CATALOG}, cuando no hay lista de precios
     * publicada y el caso de uso responde con la propuesta vacia sin invocar nada-.
     * Ese camino hoy no emite ni una senal: el producto responde 200 con cero
     * lineas a todo el mundo y la unica evidencia es que nadie compra.
     *
     * <p>
     * Las cinco degradaciones se mantienen separadas a proposito, porque tienen
     * dueno distinto: {@link #DEGRADED_SPEND_CAP} es una decision del sistema,
     * {@link #DEGRADED_MODEL_UNAVAILABLE} es una palanca de configuracion,
     * {@link #DEGRADED_NO_HINTS} es una base sin sembrar, {@link #MODEL_FAILED} es
     * una averia por la que ya se pago y {@link #NO_CATALOG} es un catalogo sin
     * publicar. Colapsarlas en un unico valor «degradado» esconde las cuatro detras
     * de la mas frecuente.
     */
    enum Outcome {

        SUCCEEDED("succeeded"),

        DEGRADED_SPEND_CAP("degraded_spend_cap"),

        DEGRADED_NO_HINTS("degraded_no_hints"),

        DEGRADED_MODEL_UNAVAILABLE("degraded_model_unavailable"),

        MODEL_FAILED("model_failed"),

        /** No hay lista de precios publicada: no se llego a invocar nada. */
        NO_CATALOG("no_catalog");

        private final String value;

        Outcome(String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }

        /**
         * El {@code switch} es exhaustivo a proposito: una constante nueva en
         * {@link GenerationOutcome} rompe la compilacion aqui en vez de caer en un
         * {@code default} que la metrica contaria como otra cosa.
         */
        public static Outcome from(GenerationOutcome outcome) {
            return switch (outcome) {
                case SUCCEEDED -> SUCCEEDED;
                case DEGRADED_SPEND_CAP -> DEGRADED_SPEND_CAP;
                case DEGRADED_NO_HINTS -> DEGRADED_NO_HINTS;
                case DEGRADED_MODEL_UNAVAILABLE -> DEGRADED_MODEL_UNAVAILABLE;
                case MODEL_FAILED -> MODEL_FAILED;
            };
        }
    }

    /**
     * Un turno servido, medido.
     *
     * @param rejectedReasons
     *            una entrada por motivo que el saneador toco, con la regla que
     *            disparo. Es vocabulario cerrado de nueve valores, nunca el texto:
     *            si la regla 3 empieza a disparar en el 40 % de las lineas el
     *            problema es el prompt, y sin contador eso no se ve
     * @param rejectedLines
     *            el veredicto de cada codigo que el modelo propuso y el motor
     *            determinista no pudo cotizar. Es la medida de si el modelo alucina
     *            codigos de catalogo
     * @param inputChars
     *            longitud del texto que escribio el prospecto en este turno.
     *            <strong>Longitud, jamas el texto</strong>
     * @param proposalId
     *            el {@code BIGINT} de {@code ai_proposals}, o {@code null} si no se
     *            persistio nada. &#9940; <strong>NO es el
     *            {@code public_token}</strong>, que es el secreto de autorizacion
     *            de la URL y no se registra en ninguna senal
     */
    record ServedProposal(Operation operation, Outcome outcome, ProposalPresentation presentation,
            List<ReasonRejection> rejectedReasons, List<LineVerdict> rejectedLines, int inputChars,
            Long proposalId) {

        public ServedProposal {
            Objects.requireNonNull(operation, "operation es obligatoria");
            Objects.requireNonNull(outcome, "outcome es obligatorio");
            Objects.requireNonNull(presentation, "presentation es obligatoria");
            rejectedReasons = rejectedReasons == null ? List.of() : List.copyOf(rejectedReasons);
            rejectedLines = rejectedLines == null ? List.of() : List.copyOf(rejectedLines);
            if (inputChars < 0) {
                throw new IllegalArgumentException("inputChars cannot be negative");
            }
        }

        /**
         * El turno que no se pudo cotizar porque no hay tarifa publicada. Se cuenta
         * igual: es el unico camino del asistente que responde 200 con cero lineas a
         * todos los prospectos a la vez.
         */
        public static ServedProposal sinCatalogo(Operation operation, int inputChars) {
            return new ServedProposal(operation, Outcome.NO_CATALOG,
                    ProposalPresentation.DETERMINISTIC, List.of(), List.of(), inputChars, null);
        }

        /**
         * Deriva la medida del turno real. Vive aqui y no en cada caso de uso para que
         * la propuesta inicial y el refinamiento no puedan medir cosas distintas: dos
         * derivaciones escritas por separado se separan.
         *
         * <p>
         * Los codigos rechazados se filtran por {@code exigeMotivo()}, es decir los que
         * puso el modelo. Los que arrastra el cierre de dependencias no son calidad del
         * modelo sino del grafo del catalogo, y mezclarlos contaminaria la unica serie
         * que mide si el modelo alucina.
         */
        public static ServedProposal de(Operation operation, GenerationOutcome outcome,
                ProposalPresentation presentation, ProposalDraft draft, CartResult cart,
                int inputChars, Long proposalId) {
            List<ReasonRejection> reglas = draft.reasons().values().stream()
                    .filter(SanitizedReason::hayQueRegistrar).map(SanitizedReason::rule).toList();
            List<LineVerdict> veredictos = cart.lineas().stream()
                    .filter(linea -> !linea.verdict().esAceptado())
                    .filter(linea -> linea.source().exigeMotivo()).map(CartLine::verdict).toList();
            return new ServedProposal(operation, Outcome.from(outcome), presentation, reglas,
                    veredictos, inputChars, proposalId);
        }
    }
}
