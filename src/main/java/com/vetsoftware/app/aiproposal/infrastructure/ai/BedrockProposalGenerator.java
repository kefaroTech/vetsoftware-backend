package com.vetsoftware.app.aiproposal.infrastructure.ai;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vetsoftware.app.aiproposal.application.dto.ModelUsage;
import com.vetsoftware.app.aiproposal.application.dto.ProposalGenerationRequest;
import com.vetsoftware.app.aiproposal.application.dto.ProposalGenerationResult;
import com.vetsoftware.app.aiproposal.application.port.out.CatalogHintQueryPort;
import com.vetsoftware.app.aiproposal.application.port.out.ProposalGeneratorPort;
import com.vetsoftware.app.aiproposal.application.port.out.SpendGuardPort;
import com.vetsoftware.app.aiproposal.application.port.out.SpendGuardPort.SpendReservation;
import com.vetsoftware.app.aiproposal.domain.GenerationOutcome;
import com.vetsoftware.app.aiproposal.domain.ModelProposalPayload;
import com.vetsoftware.app.aiproposal.domain.ProposalDraft;
import com.vetsoftware.app.aiproposal.domain.ProposalOutputValidator;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * El adaptador del puerto del modelo: decide si se invoca, con que prompt, y
 * que se hace con lo que vuelva.
 *
 * <p>
 * ⛔ <strong>NI UN {@code @Transactional} en esta clase ni en ninguna que la
 * llame.</strong> {@code SIN_IO_EXTERNO_EN_TRANSACCION} veta por prefijo los
 * paquetes de los SDK de IA y sigue la cadena de llamadas entera, asi que el
 * caso de uso que la consuma tiene que commitear el turno {@code PENDING} antes
 * y abrir TX2 despues. La secuencia no es una recomendacion: sin ella el build
 * se cae, y con ella no se retiene una conexion de Hikari durante los 3-8
 * segundos que tarda el modelo.
 *
 * <p>
 * <strong>Cuatro puertas antes de gastar un centavo</strong>, en este orden y
 * por este motivo:
 *
 * <ol>
 * <li>¿hay hints? Sin ellos no hay prompt y no se inventa uno;</li>
 * <li>¿se pudo armar el prompt?;</li>
 * <li>¿hay modelo? Se pregunta <em>antes</em> de reservar, para no ensuciar el
 * contador con reservas que se van a liberar enteras;</li>
 * <li>¿queda cupo? Fail-closed: si no se puede afirmar que si, es que no.</li>
 * </ol>
 *
 * <p>
 * <strong>Las cuatro degradan con 200, no con 500.</strong> El prospecto no
 * puede hacer nada con un error, y el carrito determinista es una propuesta
 * correcta: cierra dependencias, mete el nucleo y cotiza por tramos. Lo unico
 * que falta es la lectura del texto libre.
 *
 * <p>
 * ⚠️ <strong>El suelo de latencia de S4.2.3 ya no existe, y no hay que
 * reponerlo.</strong> Este javadoc pedia que aguas arriba se durmiera el hilo
 * 2.500-4.500 ms al degradar, para que un observador con un reloj no
 * distinguiera la ruta degradada de una generacion real. Nunca funciono: la
 * respuesta publica el estado de degradacion en su campo {@code presentation},
 * asi que el canal estaba abierto en texto plano mientras se pagaba por
 * cerrarlo. El argumento entero esta en {@code ProposalAssembler.presentacion}.
 */
@Component
public class BedrockProposalGenerator implements ProposalGeneratorPort {

    private static final Logger log = LoggerFactory.getLogger(BedrockProposalGenerator.class);

    /** Tarifa de Claude Sonnet, USD por millon de tokens (plan S7.4). */
    private static final BigDecimal USD_POR_MILLON_ENTRADA = new BigDecimal("2");

    private static final BigDecimal USD_POR_MILLON_SALIDA = new BigDecimal("10");

    private static final BigDecimal UN_MILLON = new BigDecimal("1000000");

    /**
     * La estimacion que se reserva antes de invocar: el peor caso de S7.2.1, no la
     * media. El cuarto turno acumulativo ronda los 3.800 de entrada, y el tope de
     * gasto se dimensiona con lo que un atacante va a producir a proposito.
     */
    private static final int TOKENS_ESTIMADOS_ENTRADA = 3_800;

    private static final int TOKENS_ESTIMADOS_SALIDA = 1_000;

    /**
     * &#9940; <strong>Lo que cuesta una invocacion de pago, y es publico porque hay
     * otro sitio que tiene que saberlo.</strong> El permiso diario por IP de
     * {@code LoginRateLimitFilter} se deriva de dividir el tope de gasto por este
     * numero: si se eligen por separado, se calibra el limite de peticiones por
     * encima del limite de dinero y una sola IP vacia el presupuesto de toda la
     * plataforma —que es exactamente lo que pasaba con 20/dia por IP contra un tope
     * que financiaba 18 llamadas—. {@code LoginRateLimitFilterTest} ata los dos
     * numeros; sin esta constante publica solo podria copiarlos.
     */
    public static final BigDecimal USD_ESTIMADO_POR_LLAMADA = coste(TOKENS_ESTIMADOS_ENTRADA,
            TOKENS_ESTIMADOS_SALIDA);

    /** Vocabulario cerrado de {@code failure_code}; cabe en los 40 del CHECK. */
    private static final String SALIDA_ILEGIBLE = AiErrorType.MODEL_OUTPUT_UNREADABLE.name();

    /**
     * El span de <strong>un intento</strong> contra el modelo, hijo del span del
     * caso de uso.
     *
     * <p>
     * &#9940; <strong>Un intento, un span.</strong> Asi «¿hubo reintento?» se
     * responde contando spans hermanos y no hace falta ninguna etiqueta
     * {@code retry} que multiplicaria el histograma. Y sobre todo: es el
     * <em>unico</em> sitio donde el fallo del proveedor puede marcarse como error
     * sin mentir. El span padre representa una operacion que si tuvo exito -el
     * prospecto recibio una propuesta determinista utilizable con HTTP 200-, asi
     * que marcarlo {@code Error} haria que la traza contradiga al log, al codigo de
     * estado y al SLI.
     */
    private static final String OBSERVACION_INVOCACION = "aiproposal.model.invoke";

    private static final String ERROR_TYPE = "error.type";

    /**
     * Que ya se anuncio la falta de hints. <strong>Un endpoint publico recibe
     * trafico continuo</strong>: sin esta guarda, una base recien migrada -estado
     * legitimo, no averia- escribe una linea por peticion durante dias, y un canal
     * que grita a diario ensena a ignorarlo. El hecho lo cuenta la serie
     * {@code ai_proposal_generated_total} con
     * {@code ai_outcome="degraded_no_hints"}, que no se desgasta.
     */
    private final AtomicBoolean sinHintsAnunciado = new AtomicBoolean();

    private final ModelInvoker invoker;

    private final ProposalPromptBuilder promptBuilder;

    private final CatalogHintQueryPort hintQueryPort;

    private final SpendGuardPort spendGuard;

    /**
     * <strong>Propio, no el {@code ObjectMapper} de la aplicacion.</strong> Aqui se
     * lee JSON que produce un tercero con el texto de otro tercero en contexto, y
     * la configuracion global —modulos, politicas de campos desconocidos, formatos
     * de fecha— la mueve cualquiera por motivos que no tienen nada que ver con
     * esto. Un mapeador propio hace que el comportamiento del parser sea el que
     * fija su test y no el que herede el dia que alguien registre un modulo.
     */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final Clock clock;

    private final ObservationRegistry observations;

    @SuppressWarnings("java:S107")
    public BedrockProposalGenerator(ModelInvoker invoker, ProposalPromptBuilder promptBuilder,
            CatalogHintQueryPort hintQueryPort, SpendGuardPort spendGuard, Clock clock,
            ObservationRegistry observations) {
        this.invoker = invoker;
        this.promptBuilder = promptBuilder;
        this.hintQueryPort = hintQueryPort;
        this.spendGuard = spendGuard;
        this.clock = clock;
        this.observations = observations;
    }

    @Override
    public ProposalGenerationResult generate(ProposalGenerationRequest request) {
        Map<String, String> hints = hintQueryPort.findCurrentHints();
        if (hints.isEmpty()) {
            if (sinHintsAnunciado.compareAndSet(false, true)) {
                log.info("Sin hints vigentes en catalog_item_ai_hints: las propuestas salen por"
                        + " el camino determinista. Se avisa una sola vez por proceso; el"
                        + " recuento vive en ai_proposal_generated_total con"
                        + " ai_outcome=degraded_no_hints");
            }
            return ProposalGenerationResult.degradado(GenerationOutcome.DEGRADED_NO_HINTS);
        }

        Optional<ProposalPrompt> prompt = promptBuilder.build(request, hints);
        if (prompt.isEmpty())
            return ProposalGenerationResult.degradado(GenerationOutcome.DEGRADED_NO_HINTS);

        if (!invoker.isAvailable())
            return ProposalGenerationResult.degradado(GenerationOutcome.DEGRADED_MODEL_UNAVAILABLE);

        Optional<SpendReservation> reserva = spendGuard.reserve(USD_ESTIMADO_POR_LLAMADA);
        if (reserva.isEmpty())
            return ProposalGenerationResult.degradado(GenerationOutcome.DEGRADED_SPEND_CAP);

        return invocar(request, prompt.get(), reserva.get());
    }

    /**
     * <strong>La reserva se reconcilia SIEMPRE que la invocacion llego a
     * empezar</strong>, tambien cuando fallo: el gasto ocurrio aunque no haya
     * respuesta utilizable, y cancelar en el navegador tampoco lo devuelve. Solo se
     * libera entera cuando se puede afirmar que no hubo llamada.
     */
    private ProposalGenerationResult invocar(ProposalGenerationRequest request,
            ProposalPrompt prompt, SpendReservation reserva) {
        Observation intento = Observation.createNotStarted(OBSERVACION_INVOCACION, observations)
                .contextualName("invoke model")
                .lowCardinalityKeyValue(ERROR_TYPE, AiErrorType.NONE.value()).start();
        long empezo = clock.millis();
        try (Observation.Scope alcance = intento.openScope()) {
            ModelInvoker.ModelInvocation invocacion = invoker.invoke(prompt);
            int latencia = (int) Math.max(0, clock.millis() - empezo);
            BigDecimal coste = coste(invocacion.inputTokens(), invocacion.outputTokens());
            spendGuard.reconcile(reserva, coste);

            Optional<ModelProposalPayload> payload = parsear(invocacion.rawJson());
            if (payload.isEmpty()) {
                intento.lowCardinalityKeyValue(ERROR_TYPE,
                        AiErrorType.MODEL_OUTPUT_UNREADABLE.value());
                return fallado(SALIDA_ILEGIBLE, latencia);
            }

            ProposalDraft draft = ProposalOutputValidator.validate(payload.get(),
                    request.catalog());
            return new ProposalGenerationResult(GenerationOutcome.SUCCEEDED, draft,
                    new ModelUsage(invocacion.modelId(), prompt.promptVersion(),
                            invocacion.inputTokens(), invocacion.outputTokens(), latencia,
                            invocacion.stopReason(), invocacion.rawJson(), coste),
                    null, latencia);
        } catch (ModelInvoker.ModelInvocationException fallo) {
            spendGuard.reconcile(reserva, USD_ESTIMADO_POR_LLAMADA);
            return fallado(registrar(intento, fallo.getFailureCode(), fallo),
                    (int) Math.max(0, clock.millis() - empezo));
        } catch (RuntimeException inesperado) {
            spendGuard.reconcile(reserva, USD_ESTIMADO_POR_LLAMADA);
            return fallado(registrar(intento, "MODEL_UNEXPECTED_ERROR", inesperado),
                    (int) Math.max(0, clock.millis() - empezo));
        } finally {
            intento.stop();
        }
    }

    /**
     * Marca el span del intento y escribe el evento <strong>con el nivel que decide
     * la particion de {@link AiErrorType}, no la gravedad aparente</strong>.
     *
     * <p>
     * &#9940; <strong>Ni la excepcion ni su mensaje entran en ninguna
     * senal.</strong> Un mensaje del SDK puede arrastrar el cuerpo de la peticion,
     * y el cuerpo lleva el texto del prospecto: pasarla como ultimo argumento de
     * SLF4J la escribiria en el log, y pasarla a {@code observation.error(...)} la
     * escribiria como evento {@code exception} del span, que es la mitad que nadie
     * recuerda y que ademas <strong>no</strong> pasa por {@code RedactingAppender}.
     * Lo que sale es el {@code error.type} del vocabulario cerrado y el
     * {@code failureCode} saneado a {@code [A-Z][A-Z0-9_]}, que ademas neutraliza
     * la inyeccion de log de ASVS V7.3.1.
     *
     * <p>
     * <strong>El estado del span si se fija, y a mano.</strong> Registrar la
     * excepcion y fijar el estado son dos cosas distintas en Micrometer; aqui no se
     * registra la excepcion a proposito, asi que sin este
     * {@code lowCardinalityKeyValue} mas la lectura de {@code error.type} el span
     * saldria verde con el fallo dentro y la metrica de tasa de error del proveedor
     * no existiria.
     *
     * @return el {@code failureCode} saneado, que es el que se persiste en el turno
     */
    private static String registrar(Observation intento, String failureCode,
            RuntimeException fallo) {
        AiErrorType tipo = AiErrorType.deFailureCode(failureCode);
        String codigo = AiErrorType.codigoSeguro(failureCode);
        String clase = fallo.getClass().getSimpleName();
        intento.lowCardinalityKeyValue(ERROR_TYPE, tipo.value());
        if (tipo.esSistemico()) {
            log.atError().addKeyValue("ai.error.type", tipo.value())
                    .addKeyValue("ai.failure.code", codigo).addKeyValue("ai.exception.class", clase)
                    .log("La invocacion del modelo fallo de forma determinista: fallara el 100 %"
                            + " de las propuestas hasta que una persona cambie la configuracion"
                            + " (IAM, region o habilitacion del modelo). Runbook:"
                            + " docs/ALERTAS_STACK_LOCAL.md");
            return codigo;
        }
        log.atWarn().addKeyValue("ai.error.type", tipo.value())
                .addKeyValue("ai.failure.code", codigo).addKeyValue("ai.exception.class", clase)
                .log("La invocacion del modelo fallo y la propuesta sale por el camino"
                        + " determinista; es un fallo aislado y se cura solo");
        return codigo;
    }

    /**
     * Un turno fallido lleva su {@code failureCode} y su latencia, y ni un token:
     * {@code chk_ai_proposal_turns_model_arc} no admite medidas en un turno que no
     * produjo salida.
     */
    private static ProposalGenerationResult fallado(String failureCode, int latenciaMs) {
        return new ProposalGenerationResult(GenerationOutcome.MODEL_FAILED,
                ProposalDraft.sinLineas(false, false), null,
                failureCode == null || failureCode.isBlank()
                        ? "MODEL_UNEXPECTED_ERROR"
                        : failureCode.substring(0, Math.min(40, failureCode.length())),
                latenciaMs);
    }

    /**
     * ⛔ <strong>Un JSON ilegible NO es una excepcion que suba</strong>, y tampoco
     * es un {@code understood = false}. El cuerpo lo produce un modelo con el texto
     * de un tercero en contexto, asi que recibirlo roto es un caso normal y no
     * excepcional; pero confundirlo con "el modelo dijo que no entendio" mezclaria
     * en la metrica dos poblaciones que hay que separar —una es un prospecto que
     * escribio poco, la otra es una averia nuestra que estamos pagando—. Se
     * devuelve vacio y el llamante lo traduce a {@code MODEL_FAILED}.
     */
    private static Optional<ModelProposalPayload> parsear(String rawJson) {
        if (rawJson == null || rawJson.isBlank())
            return Optional.empty();
        try {
            JsonNode raiz = MAPPER.readTree(rawJson);
            if (!raiz.isObject())
                return Optional.empty();
            Map<String, String> motivos = new LinkedHashMap<>();
            List<String> necesarios = lineas(raiz.path("necesarios"), motivos);
            List<String> recomendados = lineas(raiz.path("recomendados"), motivos);
            return Optional.of(new ModelProposalPayload(raiz.path("understood").asBoolean(false),
                    raiz.path("out_of_domain").asBoolean(false), necesarios, recomendados, motivos,
                    entero(raiz.path("usuarios")), entero(raiz.path("sedes")),
                    entero(raiz.path("cajas"))));
        } catch (JacksonException | RuntimeException ilegible) {
            log.warn("La salida del modelo no es un JSON utilizable: {}",
                    ilegible.getClass().getSimpleName());
            return Optional.empty();
        }
    }

    /**
     * Una lista del esquema del anexo E §2: un array de objetos
     * <code>{"code": …, "motivo": …}</code>, <strong>no de cadenas</strong>, y con
     * los dos campos {@code required}. El motivo viaja pegado a su codigo dentro de
     * cada elemento y no en un mapa aparte, asi que se extrae aqui al mapa que
     * espera {@link ModelProposalPayload}.
     *
     * <p>
     * <strong>Acepta ademas la forma degradada de una cadena suelta</strong>, y no
     * por indecision: el esquema se envia con {@code strict: true}, pero
     * {@code strict} lo hace cumplir el proveedor, no nosotros. Si algun dia se cae
     * —modelo distinto, ruta de reintento, una version del SDK que no lo propague—
     * la alternativa a leer el codigo sin motivo es descartar la respuesta entera y
     * cobrarla igual. El motivo ausente lo resuelve el saneador con
     * {@code short_description}, que es su trabajo.
     *
     * <p>
     * El {@code putIfAbsent} deja ganar al primero: un codigo repetido entre
     * necesarios y recomendados conserva el motivo con el que se propuso primero, y
     * la linea duplicada la marca {@code ProposalCart} con {@code DUPLICATE}.
     */
    private static List<String> lineas(JsonNode array, Map<String, String> motivos) {
        List<String> codigos = new ArrayList<>();
        if (array == null || !array.isArray())
            return codigos;
        for (JsonNode nodo : array) {
            if (nodo.isTextual()) {
                codigos.add(nodo.asText());
                continue;
            }
            JsonNode code = nodo.path("code");
            if (!nodo.isObject() || !code.isTextual())
                continue;
            codigos.add(code.asText());
            JsonNode motivo = nodo.path("motivo");
            if (motivo.isTextual())
                motivos.putIfAbsent(code.asText(), motivo.asText());
        }
        return codigos;
    }

    /**
     * Un no-entero se lee como ausencia, que {@code CapacityHint} traduce a cero.
     */
    private static Integer entero(JsonNode nodo) {
        return nodo != null && nodo.isIntegralNumber() ? nodo.asInt() : null;
    }

    /**
     * Sin tokens declarados se cobra la estimacion completa, no cero: un modelo que
     * no informa de su consumo <strong>ha consumido igual</strong>, y asumir cero
     * es exactamente como se vacia un cupo sin que el contador se mueva.
     */
    private static BigDecimal coste(Integer entrada, Integer salida) {
        int tokensEntrada = entrada == null ? TOKENS_ESTIMADOS_ENTRADA : entrada;
        int tokensSalida = salida == null ? TOKENS_ESTIMADOS_SALIDA : salida;
        return BigDecimal.valueOf(tokensEntrada).multiply(USD_POR_MILLON_ENTRADA)
                .add(BigDecimal.valueOf(tokensSalida).multiply(USD_POR_MILLON_SALIDA))
                .divide(UN_MILLON, 6, RoundingMode.HALF_UP);
    }
}
