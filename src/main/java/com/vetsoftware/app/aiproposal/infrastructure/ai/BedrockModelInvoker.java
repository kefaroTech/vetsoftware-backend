package com.vetsoftware.app.aiproposal.infrastructure.ai;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Objects;
import javax.net.ssl.SSLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.document.Document;
import software.amazon.awssdk.core.exception.ApiCallAttemptTimeoutException;
import software.amazon.awssdk.core.exception.ApiCallTimeoutException;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.AccessDeniedException;
import software.amazon.awssdk.services.bedrockruntime.model.ContentBlock;
import software.amazon.awssdk.services.bedrockruntime.model.ConversationRole;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseRequest;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseResponse;
import software.amazon.awssdk.services.bedrockruntime.model.InferenceConfiguration;
import software.amazon.awssdk.services.bedrockruntime.model.InternalServerException;
import software.amazon.awssdk.services.bedrockruntime.model.Message;
import software.amazon.awssdk.services.bedrockruntime.model.ModelErrorException;
import software.amazon.awssdk.services.bedrockruntime.model.ModelNotReadyException;
import software.amazon.awssdk.services.bedrockruntime.model.ModelTimeoutException;
import software.amazon.awssdk.services.bedrockruntime.model.ResourceNotFoundException;
import software.amazon.awssdk.services.bedrockruntime.model.ServiceUnavailableException;
import software.amazon.awssdk.services.bedrockruntime.model.StopReason;
import software.amazon.awssdk.services.bedrockruntime.model.SystemContentBlock;
import software.amazon.awssdk.services.bedrockruntime.model.ThrottlingException;
import software.amazon.awssdk.services.bedrockruntime.model.TokenUsage;
import software.amazon.awssdk.services.bedrockruntime.model.Tool;
import software.amazon.awssdk.services.bedrockruntime.model.ToolChoice;
import software.amazon.awssdk.services.bedrockruntime.model.ToolConfiguration;
import software.amazon.awssdk.services.bedrockruntime.model.ToolInputSchema;
import software.amazon.awssdk.services.bedrockruntime.model.ToolSpecification;
import software.amazon.awssdk.services.bedrockruntime.model.ToolUseBlock;
import software.amazon.awssdk.services.bedrockruntime.model.ValidationException;

/**
 * El invocador real: <strong>Bedrock Runtime, por la API
 * {@code Converse}</strong>.
 *
 * <p>
 * ⛔ <strong>Hereda la regla dura que anuncia {@link ModelInvoker}.</strong>
 * {@code SIN_IO_EXTERNO_EN_TRANSACCION} veta el paquete
 * {@code software.amazon.awssdk.services.bedrockruntime.} entero y sigue la
 * cadena de llamadas completa: un {@code @Transactional} que alcance a
 * {@link #invoke} a cualquier profundidad rompe el build. Hoy la cadena esta
 * limpia —{@code GenerateProposalService} no es transaccional a proposito y las
 * dos escrituras viven en {@code ProposalTurnWriter}, que no llama al modelo—,
 * y esta clase es lo que convierte esa regla de teorica en efectiva.
 *
 * <p>
 * <strong>Por que Bedrock y no {@code com.anthropic:anthropic-java}.</strong>
 * El contenedor se autentica hoy con el rol de tarea de ECS y sin claves: el
 * {@code DefaultCredentialsProvider} lee las credenciales del contenedor igual
 * que ya hace {@code S3Config}, y lo que la infraestructura concede es un ARN
 * de Bedrock. La API directa de Anthropic exigiria una clave de API —un secreto
 * nuevo que emitir, guardar en Secrets Manager, inyectar y rotar— y dejaria sin
 * usar el permiso que ya existe. Arquitectonicamente da igual: la regla de
 * ArchUnit veta los dos paquetes por igual.
 *
 * <p>
 * <strong>Por que {@code Converse} y no {@code InvokeModel}.</strong>
 * {@code Converse} devuelve los contadores de tokens y el motivo de parada como
 * campos de primera clase ({@link TokenUsage},
 * {@link ConverseResponse#stopReasonAsString()}) en vez de obligar a parsear a
 * mano el sobre propio del proveedor. Esos contadores <strong>no son
 * decorativos</strong>: son con lo que {@code BedrockProposalGenerator}
 * reconcilia el gasto real contra la reserva, y devolverlos nulos degrada el
 * tope diario a la estimacion.
 *
 * <p>
 * ⛔ <strong>Ni un mensaje del SDK sale de aqui.</strong> El mensaje de una
 * excepcion de Bedrock puede arrastrar el cuerpo de la peticion, y el cuerpo
 * lleva el texto que escribio el prospecto. Lo unico que cruza esta frontera es
 * un {@code failureCode} del vocabulario cerrado de {@link AiErrorType} y un
 * mensaje propio y estatico. {@link ModelInvoker.ModelInvocationException} no
 * tiene constructor con causa, asi que la cadena tampoco puede encadenarse por
 * descuido.
 *
 * <p>
 * <strong>Sin reintentos del SDK, y no por ahorro de codigo.</strong> Cada
 * intento contra el modelo <em>se cobra</em>, y el guardian reserva una vez por
 * llamada a {@link #invoke}: con la politica de reintentos por defecto —tres
 * intentos— una llamada podria costar el triple de lo reservado sin que el
 * contador se entere. Ademas {@code BedrockProposalGenerator} documenta «un
 * intento, un span»: un reintento escondido dentro del SDK seria invisible para
 * esa cuenta. Si alguna vez hacen falta reintentos, van arriba y con su span.
 *
 * <p>
 * ⛔ <strong>La salida es JSON porque se fuerza, no porque se pida por
 * favor.</strong> Hasta S10.4 el prompt no llevaba seccion de formato y el
 * parser exigia un objeto JSON pelado: un modelo real devuelve prosa y eso caia
 * en {@code MODEL_OUTPUT_UNREADABLE} <em>casi siempre</em>, es decir, la IA
 * encendida degradaba igual que apagada pero pagando cada llamada. Hoy la forma
 * la garantiza {@link ProposalOutputSchema}: se declara una herramienta con su
 * JSON Schema y se obliga a usarla con {@code toolChoice}, asi que lo que
 * vuelve es la entrada ya estructurada de esa llamada y no un parrafo del que
 * haya que rescatar un objeto.
 *
 * <p>
 * ⛔ <strong>Y el mecanismo es configuracion, no una rama a fuego, porque el uso
 * de herramientas es una capacidad POR MODELO.</strong> {@code Converse}
 * unifica la interfaz de Bedrock, no lo que sabe hacer cada familia: hay
 * modelos del catalogo que no admiten {@code toolConfig}. Atarse a la via mas
 * fuerte sin salida convierte un cambio de modelo en una rotura total en vez de
 * una degradacion. {@link StructuredOutputMode} tiene los tres escalones y
 * bajar uno es una linea de {@code application.yml}. Ademas, las instrucciones
 * piden el objeto JSON por escrito <em>en los tres</em>: el mecanismo puede no
 * estar disponible, el prompt siempre lo esta.
 *
 * <p>
 * <strong>Cuando el mecanismo no se cumple, el desenlace es declarado.</strong>
 * Si se pidio herramienta y la respuesta no trae su bloque, esto no lanza —
 * lanzar tiraria los contadores de una llamada ya cobrada— sino que devuelve
 * {@code MODEL_STRUCTURED_OUTPUT_UNSUPPORTED} en el propio
 * {@link ModelInvocation}: un codigo <em>sistemico</em>, que se escribe con
 * {@code ERROR} y tiene su propia etiqueta en el SLI. Sin el, un cambio de
 * modelo se veria exactamente igual que un prospecto que escribio raro, que es
 * el fallo que se descubre tres semanas despues mirando un panel.
 */
public class BedrockModelInvoker implements ModelInvoker {

    private static final Logger log = LoggerFactory.getLogger(BedrockModelInvoker.class);

    /**
     * &#9940; <strong>El unico canal por el que el texto del prospecto y la prosa
     * del modelo pueden salir de este proceso, y por eso tiene nombre propio en vez
     * de usar {@link #log}.</strong>
     *
     * <p>
     * Un logger aparte es lo que permite enrutarlo distinto en {@code
     * logback-spring.xml} sin tocar codigo. En local se declara con
     * {@code additivity="false"} contra {@code CONSOLE} —igual que
     * {@code DEV_EMAIL_PREVIEW}, que es el precedente— asi que se lee entero y no
     * alcanza el pipeline exportado. <strong>En dev y prod NO se declara</strong>,
     * de modo que sus eventos caen en la raiz y pasan por {@code RedactingAppender}
     * como cualquier otro: se ve la forma de la conversacion, con los datos
     * personales redactados. Es la misma politica que ya tiene escrita
     * {@code docs/POLITICA_REDACCION_LOGS.md}, no una excepcion nueva.
     *
     * <p>
     * Si se usara {@link #log}, que cuelga de {@code com.vetsoftware}, no habria
     * forma de apagar esto sin apagar tambien el aviso de fallo de la invocacion.
     */
    private static final Logger PAYLOAD = LoggerFactory.getLogger("AI_PAYLOAD");

    /**
     * El salto que parte el log en bloques legibles. Se declara en vez de
     * escribirse dentro del formato porque un mensaje multilinea es exactamente lo
     * que hace ilegible un log estructurado, y aqui esta puesto a proposito: lo que
     * se depura es texto largo que hay que LEER, no un campo que haya que
     * consultar.
     */
    private static final String SALTO = System.lineSeparator();

    /**
     * Determinismo antes que variedad. Dos turnos del golden set con el mismo
     * {@code prompt_version} y el mismo {@code catalog_snapshot_hash} tienen que
     * poder compararse; con temperatura por defecto la diferencia entre dos
     * ejecuciones no distingue «cambio el prompt» de «el modelo tuvo otro dia».
     */
    private static final float TEMPERATURA = 0f;

    /**
     * ⛔ <strong>Espejo de {@code ai_proposal_turns.stop_reason VARCHAR(30)}, y la
     * unica cosa entre el proveedor y ese {@code INSERT}.</strong> El motivo de
     * parada lo elige el proveedor de un vocabulario que crece solo: hoy el mas
     * largo que declara el SDK es {@code MODEL_CONTEXT_WINDOW_EXCEEDED}, <em>29
     * caracteres</em>, a uno del limite. Un valor nuevo y algo mas largo —o un
     * modelo de otra familia con su propio vocabulario— reventaria el
     * {@code INSERT} de TX2 <strong>despues de una llamada ya cobrada</strong>: se
     * paga el modelo y se pierde el turno entero.
     *
     * <p>
     * <strong>Se acota, no se rechaza, y aqui esta el motivo.</strong> Rechazar es
     * lo que hace {@code ProposalTurn} —el dominio tiene su espejo del mismo limite
     * y lanza—, y esta bien alli: alli un valor largo solo puede ser un error de
     * programacion de un invocador futuro. Aqui no: aqui el valor viene de fuera y
     * lanzar costaria el dinero que esta guarda existe para no perder. La regla es
     * la misma que ya aplica {@code BedrockProposalGenerator.fallado} al
     * {@code failureCode}: en la costura se acota, dentro se exige.
     */
    private static final int MAX_STOP_REASON = 30;

    /** El motivo de parada con el que un JSON truncado es normal, no una averia. */
    private static final String TRUNCADA = StopReason.MAX_TOKENS.toString();

    /**
     * <strong>Propio, no el {@code ObjectMapper} de la aplicacion</strong>, por el
     * mismo motivo que el de {@code BedrockProposalGenerator}: aqui se serializa lo
     * que produjo un tercero y el comportamiento tiene que ser el que fija su test,
     * no el que herede el dia que alguien registre un modulo global.
     */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final JsonNodeFactory NODOS = JsonNodeFactory.instance;

    private final BedrockRuntimeClient cliente;

    private final String modelId;

    private final int maxOutputTokens;

    private final StructuredOutputMode modo;

    /**
     * &#9940; <strong>APAGADO POR DEFECTO Y ESE ES EL ESTADO NORMAL.</strong>
     * Encenderlo escribe en el log el prompt entero —que lleva el texto libre del
     * prospecto— y el cuerpo entero de la respuesta. Eso es dato personal bajo la
     * Ley 1581, asi que convierte un log operativo en un almacen de datos
     * personales con su propia retencion y su propia superficie de acceso.
     *
     * <p>
     * Es un interruptor explicito y no el nivel del logger a proposito: un nivel se
     * sube de paso al depurar otra cosa, y esto no puede encenderse sin querer. Con
     * {@code false} no se construye ni la cadena del mensaje.
     */
    private final boolean logPayloads;

    public BedrockModelInvoker(BedrockRuntimeClient cliente, String modelId, int maxOutputTokens,
            StructuredOutputMode modo, boolean logPayloads) {
        this.cliente = Objects.requireNonNull(cliente, "cliente");
        this.modelId = Objects.requireNonNull(modelId, "modelId");
        this.maxOutputTokens = maxOutputTokens;
        this.modo = Objects.requireNonNull(modo, "modo");
        this.logPayloads = logPayloads;
    }

    /**
     * <strong>Siempre {@code true}: la disponibilidad se decide al cablear, no
     * aqui.</strong> Este bean solo existe cuando
     * {@code vetsoftware.ai.proposal.bedrock.enabled} vale {@code true}; sin el,
     * quien responde al puerto es {@link ModelAccessNotEnabledInvoker}, que declara
     * {@code false}. Vaciar esa variable de entorno es el kill switch de S10.4 y
     * devuelve el sistema al camino determinista sin tocar codigo.
     *
     * <p>
     * Comprobar aqui la credencial o el permiso seria peor: una llamada de sondeo
     * por propuesta cuesta latencia, y una comprobacion cacheada mentiria justo
     * cuando cambie el rol. El permiso lo dice el intento, y el intento lo
     * clasifica {@link #codigoDe}.
     */
    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public ModelInvocation invoke(ProposalPrompt prompt) {
        Objects.requireNonNull(prompt, "prompt");
        // ANTES de invocar, no despues: si la llamada revienta o se agota el
        // timeout, lo que se mando sigue siendo lo unico que explica por que.
        registrarPeticion(prompt);
        try {
            ConverseResponse respuesta = cliente.converse(peticion(prompt));
            String parada = parada(respuesta);
            Cuerpo cuerpo = cuerpo(respuesta, parada);
            registrarRespuesta(cuerpo.rawJson(), parada, respuesta);
            return new ModelInvocation(modelId, cuerpo.rawJson(), tokensDeEntrada(respuesta),
                    tokensDeSalida(respuesta), parada, cuerpo.failureCode());
        } catch (RuntimeException fallo) {
            String codigo = codigoDe(fallo);
            anotar(codigo, fallo);
            throw new ModelInvocationException(codigo,
                    "la invocacion del modelo fallo; ver ai.failure.code");
        }
    }

    /**
     * Lo que el backend le manda al modelo, literal.
     *
     * <p>
     * &#9940; <strong>Los campos se leen uno a uno y NO se usa
     * {@code prompt.toString()}.</strong> Ese {@code toString} esta escrito a
     * proposito para no imprimir el texto —solo version, hash y numero de
     * caracteres— y {@code BedrockProposalGeneratorTest} lo fija. Pasarlo por aqui
     * daria un log vacio de contenido y la sensacion de que la traza funciona.
     */
    private void registrarPeticion(ProposalPrompt prompt) {
        if (!logPayloads)
            return;
        PAYLOAD.info(
                "AI >>> peticion modelo={} version={} catalogo={} modo={}" + SALTO
                        + "--- system ---" + SALTO + "{}" + SALTO + "--- user ---" + SALTO + "{}",
                modelId, prompt.promptVersion(), prompt.catalogSnapshotHash(), modo,
                prompt.system(), prompt.user());
    }

    /**
     * Lo que el modelo contesto, y los tres datos con los que se interpreta.
     *
     * <p>
     * {@code stop} es lo primero que hay que mirar cuando el JSON no parsea: un
     * {@code max_tokens} dice que la respuesta salio truncada y que el problema
     * esta en {@code max-output-tokens}, no en el modelo. Los tokens van al lado
     * porque son con lo que se cobra: al comparar dos modelos, esta linea es la
     * medicion.
     */
    private void registrarRespuesta(String rawJson, String parada, ConverseResponse respuesta) {
        if (!logPayloads)
            return;
        PAYLOAD.info(
                "AI <<< respuesta modelo={} stop={} tokens_entrada={} tokens_salida={}" + SALTO
                        + "--- body ---" + SALTO + "{}",
                modelId, parada, tokensDeEntrada(respuesta), tokensDeSalida(respuesta), rawJson);
    }

    /** Lo que se saca de la respuesta: el cuerpo y, si lo hay, su desenlace. */
    private record Cuerpo(String rawJson, String failureCode) {
    }

    private ConverseRequest peticion(ProposalPrompt prompt) {
        ConverseRequest.Builder peticion = ConverseRequest.builder().modelId(modelId)
                .system(SystemContentBlock.fromText(prompt.system()))
                .messages(Message.builder().role(ConversationRole.USER)
                        .content(ContentBlock.fromText(prompt.user())).build())
                .inferenceConfig(InferenceConfiguration.builder().maxTokens(maxOutputTokens)
                        .temperature(TEMPERATURA).build());
        if (modo.usaHerramienta())
            peticion.toolConfig(herramienta());
        return peticion.build();
    }

    /**
     * La herramienta declarada <strong>y forzada</strong>. Declararla sin
     * {@code toolChoice} no serviria: el modelo puede elegir contestar en prosa,
     * que es exactamente el defecto que esto cierra. {@code SpecificToolChoice} le
     * quita esa opcion.
     *
     * <p>
     * {@code strict} solo viaja en {@link StructuredOutputMode#TOOL_STRICT}. Es lo
     * que hace que el proveedor <em>valide</em> la entrada contra el esquema en vez
     * de solo enseñarselo; tambien es lo primero que rechaza un modelo que no lo
     * implementa, y por eso tiene su propio escalon.
     */
    private ToolConfiguration herramienta() {
        ToolSpecification.Builder especificacion = ToolSpecification.builder()
                .name(ProposalOutputSchema.HERRAMIENTA)
                .description(ProposalOutputSchema.descripcion()).inputSchema(
                        ToolInputSchema.builder().json(ProposalOutputSchema.esquema()).build());
        if (modo.esEstricto())
            especificacion.strict(Boolean.TRUE);
        return ToolConfiguration.builder()
                .tools(Tool.builder().toolSpec(especificacion.build()).build())
                .toolChoice(ToolChoice.builder()
                        .tool(escoger -> escoger.name(ProposalOutputSchema.HERRAMIENTA)).build())
                .build();
    }

    /**
     * El cuerpo que va a {@code ModelInvocation.rawJson}, por la via que
     * corresponda al modo.
     *
     * <p>
     * <strong>«Tal cual» solo es cierto en el modo {@code PROMPT}.</strong> Esta
     * frase decia antes que el cuerpo viajaba sin recortar ni reformatear, y con la
     * herramienta forzada eso ya no puede ser: lo que llega es un {@code Document}
     * del SDK y lo que se guarda es su serializacion. Es la misma informacion,
     * escrita por nosotros; no es el byte a byte que devolvio el proveedor.
     *
     * <p>
     * <strong>Y ese cuerpo solo llega a la base de datos si parsea.</strong> El
     * javadoc de arriba lo daba por hecho, y no lo es: cuando el parser no lo puede
     * leer, {@code BedrockProposalGenerator} devuelve un resultado sin
     * {@code usage}, {@code ProposalTurnWriter} cierra el turno con
     * {@code cerrarConFallo} y ese camino <strong>no escribe
     * {@code raw_response}</strong>. Es decir, del caso que mas interesa investigar
     * —la respuesta ilegible ya pagada— no queda evidencia en la fila; queda el log
     * de esta clase y nada mas.
     *
     * <p>
     * <strong>Una salida vacia o ilegible no se convierte en excepcion
     * aqui.</strong> Lanzar perderia {@code inputTokens} y {@code outputTokens} —la
     * llamada ya se cobro— y el guardian reconciliaria contra la estimacion en vez
     * de contra el gasto real. Devolviendola, el desenlace lo pone el codigo que
     * viaja en el propio {@link ModelInvocation}, o el parser si no hay ninguno.
     */
    private Cuerpo cuerpo(ConverseResponse respuesta, String parada) {
        List<ContentBlock> bloques = bloques(respuesta);
        if (!modo.usaHerramienta())
            return new Cuerpo(prosa(bloques), null);
        for (ContentBlock bloque : bloques) {
            ToolUseBlock uso = bloque.toolUse();
            if (uso != null && ProposalOutputSchema.HERRAMIENTA.equals(uso.name()))
                return new Cuerpo(comoJson(uso.input()), null);
        }
        return sinBloqueDeHerramienta(bloques, parada);
    }

    /**
     * ⛔ <strong>Se pidio herramienta y no vino, y eso NO es «el modelo escribio
     * raro».</strong> Son dos poblaciones que hay que separar: un JSON roto es un
     * fallo aislado de una peticion y se cura solo; un modelo que ignora un
     * {@code toolChoice} forzado va a ignorarlo el 100 % de las veces hasta que
     * alguien cambie la configuracion. Mezclarlas en
     * {@code MODEL_OUTPUT_UNREADABLE} es esconder una averia total detras del ruido
     * de la aislada —el argumento entero esta en {@link AiErrorType}—, y es justo
     * lo que le pasaria a quien cambie de familia de modelo por configuracion.
     *
     * <p>
     * <strong>La excepcion es {@code max_tokens}</strong>: ahi el modelo si empezo
     * a llamar a la herramienta y se quedo sin sitio a mitad. Eso es una respuesta
     * cortada, aislada y curable subiendo {@code max-output-tokens}, asi que
     * conserva el desenlace de siempre.
     *
     * <p>
     * <strong>Y se devuelve la prosa igualmente</strong>, en vez de {@code null}:
     * es lo unico que le dice a un operador <em>que</em> contesto el modelo en
     * lugar de llamar a la herramienta.
     */
    private Cuerpo sinBloqueDeHerramienta(List<ContentBlock> bloques, String parada) {
        String prosa = prosa(bloques);
        if (TRUNCADA.equalsIgnoreCase(parada)) {
            log.warn(
                    "La respuesta se corto por longitud antes de completar la llamada a la"
                            + " herramienta {}. Es un fallo aislado; si se repite, subir"
                            + " vetsoftware.ai.proposal.bedrock.max-output-tokens",
                    ProposalOutputSchema.HERRAMIENTA);
            return new Cuerpo(prosa, null);
        }
        log.atError().addKeyValue("ai.structured.output.mode", modo.name())
                .addKeyValue("ai.model.id", modelId).addKeyValue("ai.stop.reason", parada)
                .log("El modelo NO devolvio el bloque de la herramienta {} pese a que se forzo con"
                        + " toolChoice: no admite el uso de herramientas, o no lo honra. Fallara el"
                        + " 100 % de las propuestas hasta que una persona cambie"
                        + " vetsoftware.ai.proposal.bedrock.structured-output a TOOL o a PROMPT."
                        + " Runbook: docs/ALERTAS_STACK_LOCAL.md",
                        ProposalOutputSchema.HERRAMIENTA);
        return new Cuerpo(prosa, AiErrorType.MODEL_STRUCTURED_OUTPUT_UNSUPPORTED.name());
    }

    private static List<ContentBlock> bloques(ConverseResponse respuesta) {
        if (respuesta.output() == null || respuesta.output().message() == null)
            return List.of();
        List<ContentBlock> bloques = respuesta.output().message().content();
        return bloques == null ? List.of() : bloques;
    }

    /**
     * Los bloques de texto concatenados, tal cual.
     *
     * <p>
     * ⛔ <strong>Aqui no se «rescata» el JSON de dentro de la prosa, y es una
     * decision.</strong> Recortar desde la primera llave hasta la ultima haria que
     * {@code "no se {} nada"} pasara como un objeto vacio —un
     * {@code understood=false} valido, con propuesta y con 200— y eso es prosa
     * colandose por buena, que es exactamente lo que este cambio existe para
     * impedir. Lo unico que se quita es el envoltorio de bloque de codigo, que es
     * sintaxis del sobre y no contenido; lo demas se deja fallar, porque un fallo
     * visible se arregla y un acierto por suerte no.
     */
    private static String prosa(List<ContentBlock> bloques) {
        StringBuilder cuerpo = new StringBuilder();
        for (ContentBlock bloque : bloques) {
            if (bloque.text() != null)
                cuerpo.append(bloque.text());
        }
        return cuerpo.isEmpty() ? null : sinEnvoltorio(cuerpo.toString());
    }

    /**
     * Quita el <code>```json … ```</code> que muchos modelos ponen alrededor del
     * objeto cuando el formato se pide por escrito. Es la unica concesion del modo
     * {@link StructuredOutputMode#PROMPT}, y es segura: lo que queda dentro sigue
     * teniendo que ser un objeto JSON para que el parser lo acepte, asi que no
     * puede hacer pasar prosa. Sin ella, tres caracteres de adorno tumbarian la
     * funcionalidad entera en cuanto se cambie a un modelo sin herramientas.
     */
    private static String sinEnvoltorio(String texto) {
        String limpio = texto.strip();
        if (!limpio.startsWith("```") || !limpio.endsWith("```"))
            return texto;
        int abre = limpio.indexOf('\n');
        if (abre < 0)
            return texto;
        return limpio.substring(abre + 1, limpio.length() - 3).strip();
    }

    /**
     * La entrada de la herramienta, de {@code Document} a la cadena que espera
     * {@code ModelInvocation.rawJson}.
     *
     * <p>
     * <strong>Un entero tiene que seguir siendo un nodo entero.</strong>
     * {@code BedrockProposalGenerator.entero} solo acepta un
     * {@code isIntegralNumber()}, asi que convertir todo numero a
     * {@code BigDecimal} —lo comodo— dejaria {@code usuarios}, {@code sedes} y
     * {@code cajas} en «no lo se» siempre, sin error y sin log: la nota de
     * capacidades no se pintaria nunca y nadie sabria por que.
     */
    private static String comoJson(Document entrada) {
        try {
            return MAPPER.writeValueAsString(comoNodo(entrada));
        } catch (JacksonException | RuntimeException ilegible) {
            log.warn("No se pudo serializar la entrada de la herramienta: {}",
                    ilegible.getClass().getSimpleName());
            return null;
        }
    }

    private static JsonNode comoNodo(Document documento) {
        if (documento == null || documento.isNull())
            return NODOS.nullNode();
        if (documento.isBoolean())
            return NODOS.booleanNode(documento.asBoolean());
        if (documento.isString())
            return NODOS.textNode(documento.asString());
        if (documento.isNumber())
            return numero(documento.asNumber().stringValue());
        if (documento.isList()) {
            ArrayNode array = NODOS.arrayNode();
            documento.asList().forEach(elemento -> array.add(comoNodo(elemento)));
            return array;
        }
        if (documento.isMap()) {
            ObjectNode objeto = NODOS.objectNode();
            documento.asMap().forEach((clave, valor) -> objeto.set(clave, comoNodo(valor)));
            return objeto;
        }
        return NODOS.nullNode();
    }

    /**
     * <strong>Un valor entero tiene que salir como nodo ENTERO, escriba el modelo
     * {@code 3} o {@code 3.0}.</strong> {@code BedrockProposalGenerator.entero}
     * solo acepta un {@code isIntegralNumber()}, asi que un {@code usuarios: 3.0}
     * —forma perfectamente legal en JSON, y la que emite cualquier modelo que trate
     * los numeros como coma flotante— se leeria como «no lo se» y la nota de
     * capacidades no se pintaria, sin error y sin log.
     *
     * <p>
     * Normalizar aqui y no en el parser es deliberado: el parser es comun a los
     * tres modos y a cualquier proveedor futuro, y su regla —«un no-entero es
     * ausencia»— es la correcta. Lo que hay que arreglar es la conversion, que es
     * de esta costura.
     */
    private static JsonNode numero(String texto) {
        BigDecimal valor = new BigDecimal(texto);
        try {
            return NODOS.numberNode(valor.toBigIntegerExact());
        } catch (ArithmeticException noEsEntero) {
            return NODOS.numberNode(valor);
        }
    }

    /**
     * El motivo de parada, acotado a lo que cabe en la columna. Ver
     * {@link #MAX_STOP_REASON}: el {@code INSERT} de TX2 ocurre despues de que la
     * llamada se cobro, asi que lo que no quepa se recorta aqui y se cuenta en el
     * log, en vez de tirar el turno entero por un valor nuevo del proveedor.
     */
    private static String parada(ConverseResponse respuesta) {
        String crudo = respuesta.stopReasonAsString();
        if (crudo == null || crudo.isBlank())
            return null;
        String limpio = crudo.trim();
        if (limpio.length() <= MAX_STOP_REASON)
            return limpio;
        String acotado = limpio.substring(0, MAX_STOP_REASON);
        // El motivo de parada es vocabulario del proveedor -no lleva nada del
        // prospecto-, asi que escribirlo es seguro y es lo unico que le dice a
        // alguien que hay que ampliar la columna.
        log.warn("Bedrock devolvio un stop_reason de {} caracteres y la columna admite {}; se"
                + " guarda acotado como '{}'. Hay que ampliar ai_proposal_turns.stop_reason",
                limpio.length(), MAX_STOP_REASON, acotado);
        return acotado;
    }

    private static Integer tokensDeEntrada(ConverseResponse respuesta) {
        TokenUsage uso = respuesta.usage();
        return uso == null ? null : uso.inputTokens();
    }

    private static Integer tokensDeSalida(ConverseResponse respuesta) {
        TokenUsage uso = respuesta.usage();
        return uso == null ? null : uso.outputTokens();
    }

    /**
     * La unica traza del fallo que puede escribirse, y <strong>los tres campos
     * estan elegidos uno a uno</strong>: el codigo cerrado, el nombre de la clase
     * del SDK —una constante del classpath, no un dato— y el {@code requestId} de
     * AWS, que es un identificador opaco y la unica llave con la que se abre un
     * caso de soporte. Ni el mensaje ni la excepcion como ultimo argumento de
     * SLF4J: las dos cosas escribirian el cuerpo de la peticion en el log.
     *
     * <p>
     * Va aqui y no en {@code BedrockProposalGenerator} porque el {@code requestId}
     * muere en esta frontera: lo que cruza es un codigo del vocabulario cerrado,
     * que no puede llevarlo.
     */
    private void anotar(String codigo, Throwable fallo) {
        String requestId = null;
        for (Throwable actual = fallo; actual != null
                && actual != actual.getCause(); actual = actual.getCause()) {
            if (actual instanceof AwsServiceException aws) {
                requestId = aws.requestId();
                break;
            }
        }
        log.warn("La invocacion del modelo en Bedrock fallo. code={} sdk={} awsRequestId={}",
                codigo, fallo.getClass().getSimpleName(), requestId);
        pistaDelMecanismo(codigo);
    }

    /**
     * ⛔ <strong>La linea que le ahorra el dia a quien acaba de cambiar de
     * modelo.</strong> Bedrock rechaza una peticion con {@code toolConfig} contra
     * un modelo que no admite herramientas —o con {@code strict} contra uno que no
     * lo implementa— como una {@code ValidationException} corriente, y lo unico que
     * la distingue de un {@code maxTokens} mal puesto es el texto del mensaje, que
     * es justo lo que esta clase tiene prohibido leer y escribir. Asi que el codigo
     * se queda en {@code MODEL_INVALID_REQUEST}, que es lo honesto, y lo que se
     * anade es <em>que probar</em>: no afirma la causa, la ofrece.
     *
     * <p>
     * Es gratis, ademas: una peticion rechazada por validacion no llega a inferir
     * nada y no se factura.
     */
    private void pistaDelMecanismo(String codigo) {
        if (!modo.usaHerramienta() || !AiErrorType.MODEL_INVALID_REQUEST.name().equals(codigo))
            return;
        log.atError().addKeyValue("ai.structured.output.mode", modo.name())
                .addKeyValue("ai.model.id", modelId)
                .log("La peticion llevaba toolConfig{}. Si el modelo configurado no admite el uso"
                        + " de herramientas, o no admite el esquema estricto, Bedrock la rechaza"
                        + " asi y fallara el 100 % de las propuestas: bajar"
                        + " vetsoftware.ai.proposal.bedrock.structured-output un escalon"
                        + " (TOOL_STRICT -> TOOL -> PROMPT)",
                        modo.esEstricto() ? " con strict" : "");
    }

    /**
     * Traduce la excepcion del SDK a una rama de {@link AiErrorType}, <strong>por
     * tipo y por codigo de estado, nunca por el mensaje</strong>. Buscar subcadenas
     * en el mensaje es exactamente lo que no se puede hacer: es fragil frente a
     * cualquier cambio de redaccion del proveedor y, sobre todo, mete el cuerpo de
     * la peticion —y con el el texto del prospecto— dentro de la logica de
     * clasificacion, que es el paso previo a que acabe en una senal.
     *
     * <p>
     * <strong>Un codigo por familia, no uno por excepcion.</strong> Lo que decide
     * la familia es si el mismo intento podria salir bien despues sin que nadie
     * toque nada: eso es lo que {@link AiErrorType#esSistemico()} usa para elegir
     * entre {@code WARN} y {@code ERROR}, y por eso un 429 y un 403 no pueden
     * compartir rama.
     *
     * <p>
     * <strong>Recorre la cadena de causas.</strong> El cliente sincrono envuelve
     * los fallos de transporte, asi que mirar solo la excepcion de fuera manda a
     * {@code MODEL_UNEXPECTED_ERROR} cosas perfectamente clasificables.
     *
     * <p>
     * ⛔ <strong>{@code MODEL_ACCESS_NOT_ENABLED} no se emite desde aqui.</strong>
     * Bedrock lo entrega como un {@link AccessDeniedException} corriente y lo unico
     * que lo distingue de un permiso de IAM mal puesto es el texto del mensaje —que
     * es justo lo que no se puede leer—. Ese codigo se queda como lo que siempre
     * fue: el estado <em>declarado</em> de {@link ModelAccessNotEnabledInvoker}, no
     * una inferencia sobre una cadena.
     */
    static String codigoDe(Throwable fallo) {
        for (Throwable actual = fallo; actual != null; actual = actual.getCause()) {
            String codigo = deLaCausa(actual);
            if (codigo != null)
                return codigo;
            if (actual.getCause() == actual)
                break;
        }
        return AiErrorType.MODEL_UNEXPECTED_ERROR.name();
    }

    @SuppressWarnings("java:S1479")
    private static String deLaCausa(Throwable causa) {
        return switch (causa) {
            case ThrottlingException _ -> AiErrorType.MODEL_RATE_LIMITED.name();
            case ModelTimeoutException _ -> AiErrorType.MODEL_TIMEOUT.name();
            case ModelNotReadyException _ -> AiErrorType.MODEL_OVERLOADED.name();
            case ServiceUnavailableException _ -> AiErrorType.MODEL_OVERLOADED.name();
            case InternalServerException _ -> AiErrorType.MODEL_SERVER_ERROR.name();
            case ModelErrorException _ -> AiErrorType.MODEL_SERVER_ERROR.name();
            case AccessDeniedException _ -> AiErrorType.MODEL_FORBIDDEN.name();
            case ValidationException _ -> AiErrorType.MODEL_INVALID_REQUEST.name();
            case ResourceNotFoundException _ -> AiErrorType.MODEL_INVALID_REQUEST.name();
            // Detras de las ramas concretas: cubre lo que el modelo del SDK no
            // declara -credenciales caducadas, firma invalida, cliente no
            // reconocido- sin tener que nombrarlo clase a clase.
            case AwsServiceException aws -> porEstado(aws.statusCode());
            case ApiCallTimeoutException _ -> AiErrorType.MODEL_TIMEOUT.name();
            case ApiCallAttemptTimeoutException _ -> AiErrorType.MODEL_TIMEOUT.name();
            case SocketTimeoutException _ -> AiErrorType.MODEL_TIMEOUT.name();
            case UnknownHostException _ -> AiErrorType.MODEL_CONNECTION_ERROR.name();
            case ConnectException _ -> AiErrorType.MODEL_CONNECTION_ERROR.name();
            case SSLException _ -> AiErrorType.MODEL_CONNECTION_ERROR.name();
            case IOException _ -> AiErrorType.MODEL_CONNECTION_ERROR.name();
            default -> null;
        };
    }

    /**
     * El estado HTTP es la senal mas fiable de lo que le paso a una peticion que si
     * llego al servicio, y no puede llevar datos del prospecto.
     */
    private static String porEstado(int estado) {
        return switch (estado) {
            case 401 -> AiErrorType.MODEL_UNAUTHORIZED.name();
            case 403 -> AiErrorType.MODEL_FORBIDDEN.name();
            case 408 -> AiErrorType.MODEL_TIMEOUT.name();
            case 429 -> AiErrorType.MODEL_RATE_LIMITED.name();
            case 503 -> AiErrorType.MODEL_OVERLOADED.name();
            default -> porFamilia(estado);
        };
    }

    private static String porFamilia(int estado) {
        if (estado >= 500)
            return AiErrorType.MODEL_SERVER_ERROR.name();
        if (estado >= 400)
            return AiErrorType.MODEL_INVALID_REQUEST.name();
        return AiErrorType.MODEL_UNEXPECTED_ERROR.name();
    }
}
