package com.vetsoftware.app.aiproposal.application.port.out;

import com.vetsoftware.app.aiproposal.application.dto.ProposalGenerationResult;
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

        /**
         * &#9940; <strong>No hay ninguna lista de precios {@code PUBLISHED}
         * vigente.</strong> No se llego a invocar nada. La accion es <em>publicar la
         * tarifa</em> desde la consola de plataforma con una cuenta real.
         */
        NO_CATALOG("no_catalog"),

        /**
         * &#9940; <strong>Hay lista publicada, pero el catalogo vendible sale
         * vacio.</strong> Tampoco se invoca nada y el prospecto ve lo mismo —cero
         * lineas—, pero <strong>la accion es la contraria</strong>: la tarifa ya esta
         * publicada, asi que lo que hay que averiguar es por que no cuelga de ella
         * ningun articulo vendible (todos deshabilitados, todos fuera de
         * {@code ACTIVE}, o ninguno con tramo para el ciclo pedido).
         *
         * <p>
         * <strong>Separado de {@link #NO_CATALOG} justamente por eso.</strong> Con los
         * dos caminos colapsados en un mismo valor, la alerta diria «publica la tarifa»
         * a quien ya la tiene publicada, y quien la recibe pierde el turno comprobando
         * algo que ya esta bien. Dos poblaciones con dueno y con remedio distintos no
         * pueden compartir desenlace.
         */
        EMPTY_CATALOG("empty_catalog");

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
     * &#9940; <strong>La particion del fallo del modelo en las DOS poblaciones que
     * tienen dueno distinto, y solo dos.</strong>
     *
     * <p>
     * {@code ai_outcome="model_failed"} colapsa hoy dos cosas que se atienden al
     * reves: un tiempo agotado o un limite de tasa fallan peticiones sueltas y se
     * curan solos —no hay nadie a quien despertar—, mientras que unas credenciales
     * o un permiso mal puestos fallan el <strong>100 %</strong> de las propuestas
     * hasta que una persona cambie configuracion. La distincion ya existia, pero
     * <em>solo en el nivel del log</em>: quien mira el contador no podia verla.
     *
     * <p>
     * <strong>Dos valores utiles y no los trece de {@code AiErrorType}.</strong> El
     * arbol de decision de quien recibe la alerta tiene exactamente dos ramas
     * —«espera» o «entra a mirar configuracion»—, y multiplicar la cardinalidad de
     * la serie por trece para responder una pregunta binaria es pagar
     * almacenamiento por ruido. El codigo exacto sigue estando donde se puede
     * consultar de uno en uno: {@code error.type} en el span del intento.
     *
     * <p>
     * ⛔ <strong>{@link #NONE} existe por una razon tecnica, no de negocio, y no se
     * puede quitar.</strong> {@code PrometheusMeterRegistry} exige que todas las
     * muestras de un mismo medidor lleven <em>el mismo juego de claves de
     * etiqueta</em>: emitir la etiqueta solo en los turnos que fallaron reventaria
     * el registro. Es el mismo motivo por el que {@code AiErrorType.NONE} existe
     * para {@code error.type}. Una alerta que quiera solo fallos filtra
     * {@code ai_failure_kind="systemic"}.
     *
     * <p>
     * Que esto se comprueba de verdad no es suerte:
     * {@code MicrometerAiProposalMetricsTest} monta un
     * {@code PrometheusMeterRegistry} y no un {@code SimpleMeterRegistry}, que es
     * lo unico que hace que la incoherencia de claves salte en la prueba y no en el
     * primer arranque de produccion.
     */
    enum FailureKind {

        /** No hubo fallo del modelo en este turno. El camino normal. */
        NONE("none"),

        /**
         * Se curara solo: tiempo agotado, limite de tasa, error del servidor, salida
         * ilegible. No hay nadie a quien despertar; lo que se vigila es la
         * <em>tasa</em>, no el evento.
         */
        TRANSIENT("transient"),

        /**
         * No se cura solo. Credenciales, permisos, acceso al modelo, peticion invalida,
         * o un codigo sin rama. Falla el 100 % hasta que alguien cambie configuracion,
         * asi que un solo evento ya es accionable.
         */
        SYSTEMIC("systemic");

        private final String value;

        FailureKind(String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }
    }

    /**
     * Un turno servido, medido.
     *
     * @param failureCode
     *            el {@code failureCode} crudo del generador, o {@code null} en el
     *            camino normal. &#9940; <strong>Viaja como cadena y no como
     *            {@code AiErrorType} a proposito</strong>: ese enum vive en
     *            {@code infrastructure.ai} y este puerto es de {@code application},
     *            asi que importarlo invertiria la direccion de dependencias y rompe
     *            ArchUnit. Quien lo traduce a {@link FailureKind} es el adaptador
     *            de metricas, que si es infraestructura
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
            String failureCode, List<ReasonRejection> rejectedReasons,
            List<LineVerdict> rejectedLines, int inputChars, Long proposalId) {

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
         *
         * <p>
         * &#9940; <strong>La etiqueta {@code ai.presentation} vale {@code no_catalog},
         * no {@code deterministic}.</strong> Con la anterior, la serie decia que se
         * habia servido el carrito determinista —el que SI lleva lineas— justo en el
         * unico camino donde no se sirvio ninguna, y un panel filtrado por presentacion
         * contaba estas peticiones dentro de la poblacion sana. Ahora las dos etiquetas
         * del contador dicen lo mismo y se pueden cruzar sin que una desmienta a la
         * otra.
         */
        public static ServedProposal sinCatalogo(Operation operation, int inputChars) {
            return sinCotizar(operation, Outcome.NO_CATALOG, inputChars);
        }

        /**
         * El turno con la tarifa <strong>ya publicada</strong> y ni un articulo
         * vendible colgando de ella.
         *
         * <p>
         * &#9940; <strong>Mismo sintoma que {@link #sinCatalogo}, remedio
         * contrario.</strong> El prospecto ve lo mismo —200 con cero lineas—, pero
         * quien recibe la alerta tiene que hacer lo opuesto: alli hay que publicar la
         * tarifa, aqui la tarifa ya esta publicada y lo que falta es averiguar por que
         * no cuelga de ella ningun articulo. Compartir desenlace mandaba a esa persona
         * a comprobar algo que ya estaba bien.
         */
        public static ServedProposal catalogoVacio(Operation operation, int inputChars) {
            return sinCotizar(operation, Outcome.EMPTY_CATALOG, inputChars);
        }

        /**
         * Los dos caminos en los que <strong>no se cotizo nada</strong>. La
         * presentacion es {@code NO_CATALOG} en los dos porque describe lo que el
         * prospecto ve —nada—, y lo que los separa es el {@code outcome}, que es lo que
         * describe que hay que hacer.
         */
        private static ServedProposal sinCotizar(Operation operation, Outcome outcome,
                int inputChars) {
            return new ServedProposal(operation, outcome, ProposalPresentation.NO_CATALOG, null,
                    List.of(), List.of(), inputChars, null);
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
         *
         * <p>
         * &#9940; <strong>Recibe el {@link ProposalGenerationResult} entero y ya no el
         * par {@code outcome} + {@code draft} suelto.</strong> El {@code failureCode}
         * vive en ese mismo objeto, y pasarlo como un tercer parametro invitaba a que
         * un llamante futuro olvidara justo ese: el desenlace habria seguido saliendo
         * bien y la clase del fallo habria caido en silencio a {@code none}, que es una
         * mentira que no rompe nada. Con el resultado completo no hay forma de
         * despareja los tres.
         */
        public static ServedProposal de(Operation operation, ProposalGenerationResult resultado,
                ProposalPresentation presentation, CartResult cart, int inputChars,
                Long proposalId) {
            ProposalDraft draft = resultado.draft();
            List<ReasonRejection> reglas = draft.reasons().values().stream()
                    .filter(SanitizedReason::hayQueRegistrar).map(SanitizedReason::rule).toList();
            List<LineVerdict> veredictos = cart.lineas().stream()
                    .filter(linea -> !linea.verdict().esAceptado())
                    .filter(linea -> linea.source().exigeMotivo()).map(CartLine::verdict).toList();
            return new ServedProposal(operation, Outcome.from(resultado.outcome()), presentation,
                    resultado.failureCode(), reglas, veredictos, inputChars, proposalId);
        }
    }
}
