package com.vetsoftware.app.aiproposal.infrastructure.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vetsoftware.app.aiproposal.infrastructure.ai.ModelInvoker.ModelInvocation;
import com.vetsoftware.app.aiproposal.infrastructure.ai.ModelInvoker.ModelInvocationException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.document.Document;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.AccessDeniedException;
import software.amazon.awssdk.services.bedrockruntime.model.ContentBlock;
import software.amazon.awssdk.services.bedrockruntime.model.ConversationRole;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseOutput;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseRequest;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseResponse;
import software.amazon.awssdk.services.bedrockruntime.model.InternalServerException;
import software.amazon.awssdk.services.bedrockruntime.model.Message;
import software.amazon.awssdk.services.bedrockruntime.model.ModelNotReadyException;
import software.amazon.awssdk.services.bedrockruntime.model.ModelTimeoutException;
import software.amazon.awssdk.services.bedrockruntime.model.ResourceNotFoundException;
import software.amazon.awssdk.services.bedrockruntime.model.ServiceUnavailableException;
import software.amazon.awssdk.services.bedrockruntime.model.StopReason;
import software.amazon.awssdk.services.bedrockruntime.model.ThrottlingException;
import software.amazon.awssdk.services.bedrockruntime.model.TokenUsage;
import software.amazon.awssdk.services.bedrockruntime.model.ToolUseBlock;
import software.amazon.awssdk.services.bedrockruntime.model.ValidationException;

/**
 * La costura con Bedrock, probada entera y sin red.
 *
 * <p>
 * <b>El test que de verdad importa es el del senuelo.</b> El contrato de
 * {@link ModelInvoker} prohibe propagar el mensaje del SDK porque ese mensaje
 * puede arrastrar el cuerpo de la peticion, y el cuerpo lleva el texto que
 * escribio el prospecto. Aqui se monta exactamente ese escenario —una excepcion
 * de Bedrock cuyo mensaje trae el cuerpo entero— y se comprueba que el senuelo
 * no sale por <em>ninguna</em> de las tres superficies que esta clase controla:
 * el mensaje de la excepcion que se lanza, su cadena de causas y el log.
 *
 * <p>
 * <b>Y se comprueba que el andamiaje no da falsos verdes:</b> el senuelo tiene
 * que estar de verdad en el mensaje del SDK, o los {@code doesNotContain}
 * pasarian por vacuidad.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BedrockModelInvoker — el mensaje del SDK no cruza, los tokens si")
class BedrockModelInvokerTest {

    /**
     * Improbable a proposito y en una sola pieza; ver AiProposalTelemetryLeakTest.
     */
    private static final String SENUELO = "Qx7ZtVeterinariaSanMarcosQx7Zt";

    /**
     * Lo que un SDK real mete en el mensaje de su excepcion: la peticion entera.
     */
    private static final String MENSAJE_DEL_SDK = "400 Bad Request; body={\"messages\":[{\"role\":"
            + "\"user\",\"content\":\"Somos " + SENUELO + " de Chapinero\"}]}";

    private static final String MODEL_ID = "us.anthropic.claude-sonnet-4-5-20250929-v1:0";

    private static final int MAX_TOKENS = 1500;

    /** El modo de por defecto, que es el que se despliega. */
    private static final StructuredOutputMode MODO = StructuredOutputMode.TOOL_STRICT;

    /**
     * El mismo Jackson que usa el generador para decidir si la salida es legible.
     */
    private static final ObjectMapper JACKSON = new ObjectMapper();

    private static final ProposalPrompt PROMPT = new ProposalPrompt("instrucciones y catalogo",
            "TEXTO DEL CLIENTE: Somos " + SENUELO, "v1", "a".repeat(64));

    @Mock
    private BedrockRuntimeClient cliente;

    private BedrockModelInvoker invocador;

    private ListAppender<ILoggingEvent> logs;

    private Logger raiz;

    private ListAppender<ILoggingEvent> delPayload;

    private Logger canalDelPayload;

    private Level nivelPrevioDelCanal;

    @BeforeEach
    void montar() {
        invocador = new BedrockModelInvoker(cliente, MODEL_ID, MAX_TOKENS, MODO, false);
        LoggerContext contexto = (LoggerContext) LoggerFactory.getILoggerFactory();

        // En la RAIZ y no en el logger de la clase: la fuga que se busca es la que
        // escribe alguien que no sabe que existe esta regla.
        raiz = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        logs = new ListAppender<>();
        logs.setContext(contexto);
        logs.start();
        raiz.addAppender(logs);

        // ⛔ EL CANAL DEL PAYLOAD SE LEE DE SU PROPIO LOGGER, NO DE LA RAIZ, y esto no
        // es una preferencia de estilo: es el fallo que puso `develop` en rojo.
        //
        // logback-spring.xml declara AI_PAYLOAD con additivity="false" -es lo que
        // impide que el prompt del prospecto alcance el pipeline exportado, y
        // LogbackRedactionConfigTest lo exige-. En cuanto CUALQUIER rodaja de Spring
        // del mismo fork carga esa configuracion -y solo dos clases del arbol fijan
        // @ActiveProfiles, asi que casa el perfil local-, los eventos del canal dejan
        // de propagar a la raiz PARA EL RESTO DE LA JVM. Leyendolos desde la raiz,
        // estas pruebas pasaban en aislamiento y contaban CERO en la suite completa,
        // que es exactamente el peor modo de fallar: verde cuando se depura, rojo
        // cuando se integra, y el sintoma -"el log no escribe nada"- señalando a una
        // implementacion que funciona.
        //
        // Enganchado al propio logger, el resultado no depende ya de que otra clase
        // haya reconfigurado el contexto antes. Mismo patron que DevEmailPreviewTest,
        // el otro canal sin redaccion, que por eso nunca sufrio esto.
        canalDelPayload = contexto.getLogger("AI_PAYLOAD");
        nivelPrevioDelCanal = canalDelPayload.getLevel();
        canalDelPayload.setLevel(Level.INFO);
        delPayload = new ListAppender<>();
        delPayload.setContext(contexto);
        delPayload.start();
        canalDelPayload.addAppender(delPayload);
    }

    @AfterEach
    void desmontar() {
        raiz.detachAppender(logs);
        logs.stop();
        canalDelPayload.detachAppender(delPayload);
        canalDelPayload.setLevel(nivelPrevioDelCanal);
        delPayload.stop();
    }

    @Nested
    @DisplayName("R1 — el mensaje del SDK no sale de la costura")
    class NoFiltra {

        @Test
        @DisplayName("el mensaje del SDK lleva el texto del prospecto y NO aparece ni en la excepcion, ni en su causa, ni en el log")
        void el_mensaje_del_sdk_no_cruza() {
            when(cliente.converse(any(ConverseRequest.class)))
                    .thenThrow(ValidationException.builder().message(MENSAJE_DEL_SDK).build());

            ModelInvocationException fallo = catchThrowableOfType(ModelInvocationException.class,
                    () -> invocador.invoke(PROMPT));

            assertThat(fallo.getMessage()).as("mensaje de la excepcion que cruza la frontera")
                    .doesNotContain(SENUELO);
            assertThat(cadena(fallo)).as("cadena de causas").doesNotContain(SENUELO);
            for (ILoggingEvent evento : logs.list) {
                assertThat(evento.getFormattedMessage()).as("mensaje de log")
                        .doesNotContain(SENUELO);
                assertThat(cadena(evento.getThrowableProxy() == null ? null : fallo))
                        .as("excepcion adjunta al log").doesNotContain(SENUELO);
            }
        }

        @Test
        @DisplayName("la excepcion no encadena la del SDK: una causa la reimprimiria entera aguas arriba")
        void no_encadena_la_causa() {
            when(cliente.converse(any(ConverseRequest.class)))
                    .thenThrow(ValidationException.builder().message(MENSAJE_DEL_SDK).build());

            ModelInvocationException fallo = catchThrowableOfType(ModelInvocationException.class,
                    () -> invocador.invoke(PROMPT));

            assertThat(fallo.getCause()).isNull();
        }

        @Test
        @DisplayName("el log del fallo si deja la llave de soporte: codigo, clase del SDK y awsRequestId")
        void el_log_deja_la_llave_de_soporte() {
            when(cliente.converse(any(ConverseRequest.class))).thenThrow(ThrottlingException
                    .builder().message(MENSAJE_DEL_SDK).requestId("req-0f3a").build());

            assertThatThrownBy(() -> invocador.invoke(PROMPT))
                    .isInstanceOf(ModelInvocationException.class);

            assertThat(logs.list).anyMatch(evento -> {
                String linea = evento.getFormattedMessage();
                return linea.contains("MODEL_RATE_LIMITED") && linea.contains("ThrottlingException")
                        && linea.contains("req-0f3a");
            });
        }

        @Test
        @DisplayName("el andamiaje no da falsos verdes: el senuelo SI esta en el mensaje del SDK")
        void el_andamiaje_no_miente() {
            assertThat(ValidationException.builder().message(MENSAJE_DEL_SDK).build().getMessage())
                    .contains(SENUELO);
        }
    }

    @Nested
    @DisplayName("Los cinco campos del record")
    class Respuesta {

        @Test
        @DisplayName("los contadores de tokens llegan al record: con ellos el guardian reconcilia el gasto real")
        void los_tokens_llegan() {
            when(cliente.converse(any(ConverseRequest.class)))
                    .thenReturn(respuesta("{\"understood\":true}", 3200, 900, StopReason.END_TURN));

            ModelInvocation invocacion = invocador.invoke(PROMPT);

            assertThat(invocacion.inputTokens()).isEqualTo(3200);
            assertThat(invocacion.outputTokens()).isEqualTo(900);
        }

        @Test
        @DisplayName("el rawJson es el cuerpo tal cual: ni recortado ni reformateado")
        void el_raw_json_va_tal_cual() {
            String cuerpo = "  {\"understood\": true,\n \"necesarios\": []}  \n";
            when(cliente.converse(any(ConverseRequest.class)))
                    .thenReturn(respuesta(cuerpo, 10, 20, StopReason.END_TURN));

            assertThat(invocador.invoke(PROMPT).rawJson()).isEqualTo(cuerpo);
        }

        @Test
        @DisplayName("el modelId y el stopReason son los que hay que persistir")
        void el_model_id_y_el_stop_reason() {
            when(cliente.converse(any(ConverseRequest.class)))
                    .thenReturn(respuesta("{}", 1, 2, StopReason.MAX_TOKENS));

            ModelInvocation invocacion = invocador.invoke(PROMPT);

            assertThat(invocacion.modelId()).isEqualTo(MODEL_ID);
            assertThat(invocacion.stopReason()).isEqualTo("max_tokens");
        }

        @Test
        @DisplayName("una salida vacia NO lanza: lanzar tiraria los tokens de una llamada ya cobrada")
        void la_salida_vacia_conserva_los_tokens() {
            when(cliente.converse(any(ConverseRequest.class))).thenReturn(ConverseResponse.builder()
                    .output(ConverseOutput.fromMessage(Message.builder()
                            .role(ConversationRole.ASSISTANT).content(List.of()).build()))
                    .usage(TokenUsage.builder().inputTokens(3200).outputTokens(0).totalTokens(3200)
                            .build())
                    .stopReason(StopReason.END_TURN).build());

            ModelInvocation invocacion = invocador.invoke(PROMPT);

            assertThat(invocacion.rawJson()).isNull();
            assertThat(invocacion.inputTokens()).isEqualTo(3200);
        }

        @Test
        @DisplayName("el texto del prospecto va en el turno user y el system queda estable")
        void el_texto_del_cliente_va_en_el_turno_user() {
            when(cliente.converse(any(ConverseRequest.class)))
                    .thenReturn(respuesta("{}", 1, 2, StopReason.END_TURN));

            invocador.invoke(PROMPT);

            ArgumentCaptor<ConverseRequest> captor = ArgumentCaptor.forClass(ConverseRequest.class);
            org.mockito.Mockito.verify(cliente).converse(captor.capture());
            ConverseRequest peticion = captor.getValue();
            assertThat(peticion.modelId()).isEqualTo(MODEL_ID);
            assertThat(peticion.system().getFirst().text()).isEqualTo(PROMPT.system())
                    .doesNotContain(SENUELO);
            assertThat(peticion.messages().getFirst().content().getFirst().text())
                    .isEqualTo(PROMPT.user());
            assertThat(peticion.inferenceConfig().maxTokens()).isEqualTo(MAX_TOKENS);
        }
    }

    @Nested
    @DisplayName("Cada familia de fallo del SDK cae en su codigo del vocabulario cerrado")
    class Codigos {

        @ParameterizedTest(name = "{1} -> {2}")
        @MethodSource("com.vetsoftware.app.aiproposal.infrastructure.ai.BedrockModelInvokerTest#familias")
        @DisplayName("la excepcion del SDK se traduce a la rama de AiErrorType que le toca")
        void cada_familia_su_codigo(RuntimeException delSdk, String etiqueta, String esperado) {
            when(cliente.converse(any(ConverseRequest.class))).thenThrow(delSdk);

            ModelInvocationException fallo = catchThrowableOfType(ModelInvocationException.class,
                    () -> invocador.invoke(PROMPT));

            assertThat(fallo.getFailureCode()).as(etiqueta).isEqualTo(esperado);
            // Y el codigo tiene que ser vocabulario que el generador sepa leer: si no,
            // AiErrorType lo manda a OTHER y el nivel de log deja de distinguir
            // fallo aislado de averia total.
            assertThat(AiErrorType.deFailureCode(esperado)).isNotEqualTo(AiErrorType.OTHER);
        }
    }

    static Stream<Arguments> familias() {
        return Stream.of(
                Arguments.of(AccessDeniedException.builder().message(MENSAJE_DEL_SDK).build(),
                        "permisos", "MODEL_FORBIDDEN"),
                Arguments.of(porEstado(401), "credenciales (401 generico)", "MODEL_UNAUTHORIZED"),
                Arguments.of(porEstado(403), "permisos (403 generico)", "MODEL_FORBIDDEN"),
                Arguments.of(ThrottlingException.builder().message(MENSAJE_DEL_SDK).build(),
                        "limite de tasa del proveedor", "MODEL_RATE_LIMITED"),
                Arguments.of(porEstado(429), "limite de tasa (429 generico)", "MODEL_RATE_LIMITED"),
                Arguments.of(ModelTimeoutException.builder().message(MENSAJE_DEL_SDK).build(),
                        "tiempo agotado en el modelo", "MODEL_TIMEOUT"),
                Arguments.of(
                        SdkClientException.builder().cause(new SocketTimeoutException("read"))
                                .build(),
                        "tiempo agotado en el socket (envuelto)", "MODEL_TIMEOUT"),
                Arguments.of(SdkClientException.builder().cause(new UnknownHostException("bedrock"))
                        .build(), "DNS (envuelto)", "MODEL_CONNECTION_ERROR"),
                Arguments.of(
                        SdkClientException.builder().cause(new ConnectException("rechazado"))
                                .build(),
                        "conexion rechazada (envuelta)", "MODEL_CONNECTION_ERROR"),
                Arguments.of(InternalServerException.builder().message(MENSAJE_DEL_SDK).build(),
                        "error del servicio", "MODEL_SERVER_ERROR"),
                Arguments.of(ServiceUnavailableException.builder().message(MENSAJE_DEL_SDK).build(),
                        "servicio saturado", "MODEL_OVERLOADED"),
                Arguments.of(ModelNotReadyException.builder().message(MENSAJE_DEL_SDK).build(),
                        "modelo no listo", "MODEL_OVERLOADED"),
                Arguments.of(ValidationException.builder().message(MENSAJE_DEL_SDK).build(),
                        "peticion invalida", "MODEL_INVALID_REQUEST"),
                Arguments.of(ResourceNotFoundException.builder().message(MENSAJE_DEL_SDK).build(),
                        "modelo o perfil inexistente", "MODEL_INVALID_REQUEST"),
                Arguments.of(new IllegalStateException(MENSAJE_DEL_SDK), "sin clasificar",
                        "MODEL_UNEXPECTED_ERROR"));
    }

    @Nested
    @DisplayName("El cableado no puede dejar el puerto sin bean")
    class Cableado {

        @Test
        @DisplayName("las dos condiciones son complementarias exactas: si las dos fallaran, el contexto no levanta")
        void las_condiciones_son_complementarias() {
            String delFallback = BedrockDisabledInvoker.class.getAnnotation(
                    org.springframework.boot.autoconfigure.condition.ConditionalOnExpression.class)
                    .value();

            assertThat(delFallback).isEqualTo("!(" + BedrockInvokerConfig.ACTIVO + ")");
        }

        @Test
        @DisplayName("el invocador de Bedrock se declara disponible: quien decide es la condicion del cableado")
        void esta_disponible() {
            assertThat(invocador.isAvailable()).isTrue();
        }
    }

    /**
     * <b>El bloque que separa «la IA funciona» de «la IA degrada pagando».</b>
     * Antes de esto el prompt no pedia el formato y el parser exigia un objeto JSON
     * pelado: un modelo real contesta en prosa y eso caia en
     * {@code MODEL_OUTPUT_UNREADABLE} casi siempre.
     *
     * <p>
     * <b>Y por eso los dos controles van juntos.</b> «La prosa no pasa» sin «lo
     * bien formado si pasa» no distingue un mecanismo que funciona de uno que
     * rechaza todo, que es la forma mas facil de tener un test verde que no
     * comprueba nada.
     */
    @Nested
    @DisplayName("La salida estructurada — JSON de verdad, no por suerte")
    class SalidaEstructurada {

        @Test
        @DisplayName("la herramienta se declara Y se fuerza: declararla sin toolChoice deja al modelo elegir prosa igual")
        void la_herramienta_va_forzada() {
            when(cliente.converse(any(ConverseRequest.class)))
                    .thenReturn(conHerramienta(entradaCompleta(), StopReason.TOOL_USE));

            invocador.invoke(PROMPT);

            ConverseRequest peticion = capturada();
            assertThat(peticion.toolConfig()).as("toolConfig de la peticion").isNotNull();
            assertThat(peticion.toolConfig().tools()).hasSize(1);
            assertThat(peticion.toolConfig().tools().getFirst().toolSpec().name())
                    .isEqualTo(ProposalOutputSchema.HERRAMIENTA);
            // Sin esto la herramienta es una sugerencia y el defecto sigue vivo.
            assertThat(peticion.toolConfig().toolChoice().tool()).as("toolChoice forzado")
                    .isNotNull();
            assertThat(peticion.toolConfig().toolChoice().tool().name())
                    .isEqualTo(ProposalOutputSchema.HERRAMIENTA);
            assertThat(peticion.toolConfig().tools().getFirst().toolSpec().strict())
                    .as("strict en el modo TOOL_STRICT").isTrue();
        }

        @Test
        @DisplayName("el esquema declara los siete campos del anexo E, todos obligatorios y sin propiedades de mas")
        void el_esquema_es_el_del_anexo_e() {
            when(cliente.converse(any(ConverseRequest.class)))
                    .thenReturn(conHerramienta(entradaCompleta(), StopReason.TOOL_USE));

            invocador.invoke(PROMPT);

            Map<String, Document> esquema = capturada().toolConfig().tools().getFirst().toolSpec()
                    .inputSchema().json().asMap();
            assertThat(esquema.get("properties").asMap().keySet())
                    .isEqualTo(ProposalOutputSchema.CAMPOS);
            // strict solo garantiza la forma si TODO es obligatorio y no se admiten
            // propiedades de mas: con required incompleto, el proveedor no valida.
            assertThat(nombres(esquema.get("required")))
                    .containsExactlyInAnyOrderElementsOf(ProposalOutputSchema.CAMPOS);
            assertThat(esquema.get("additionalProperties").asBoolean()).isFalse();

            Map<String, Document> linea = esquema.get("properties").asMap().get("necesarios")
                    .asMap().get("items").asMap();
            assertThat(linea.get("properties").asMap().keySet())
                    .isEqualTo(ProposalOutputSchema.CAMPOS_DE_LINEA);
            // El motivo viaja DENTRO de cada elemento; en un mapa aparte, un modelo
            // que devolviera lista y mapa desalineados pondria el motivo de otra.
            assertThat(nombres(linea.get("required")))
                    .containsExactlyInAnyOrderElementsOf(ProposalOutputSchema.CAMPOS_DE_LINEA);
            assertThat(linea.get("additionalProperties").asBoolean()).isFalse();
        }

        @Test
        @DisplayName("CONTROL POSITIVO: una llamada bien formada SI produce un rawJson que el parser lee")
        void una_respuesta_bien_formada_si_pasa() {
            when(cliente.converse(any(ConverseRequest.class)))
                    .thenReturn(conHerramienta(entradaCompleta(), StopReason.TOOL_USE));

            ModelInvocation invocacion = invocador.invoke(PROMPT);

            assertThat(invocacion.failureCode()).isNull();
            JsonNode leido = leer(invocacion.rawJson());
            assertThat(leido).as("el cuerpo tiene que ser un objeto JSON").isNotNull();
            assertThat(leido.isObject()).isTrue();
            assertThat(leido.path("understood").asBoolean()).isTrue();
            assertThat(leido.path("necesarios").get(0).path("code").asText()).isEqualTo("CORE");
            assertThat(leido.path("necesarios").get(0).path("motivo").asText())
                    .isEqualTo("Es la base y va contigo");
        }

        @Test
        @DisplayName("un 3.0 del modelo sigue siendo un 3 entero: como decimal, esa capacidad seria «no lo se» sin error y sin log")
        void un_entero_escrito_como_decimal_sobrevive() {
            // 3.0 es JSON legal y es lo que emite cualquier modelo que trate los
            // numeros como coma flotante. BedrockProposalGenerator.entero solo acepta
            // isIntegralNumber(), asi que sin normalizar aqui la nota de capacidades
            // no se pintaria nunca y nada lo delataria.
            Document conDecimales = Document.mapBuilder().putBoolean("understood", true)
                    .putBoolean("out_of_domain", false).putList("necesarios", List.of())
                    .putList("recomendados", List.of())
                    .putNumber("usuarios", new java.math.BigDecimal("3.0")).putNumber("sedes", 1)
                    .putNumber("cajas", 0).build();
            when(cliente.converse(any(ConverseRequest.class)))
                    .thenReturn(conHerramienta(conDecimales, StopReason.TOOL_USE));

            JsonNode leido = leer(invocador.invoke(PROMPT).rawJson());

            assertThat(leido.path("usuarios").isIntegralNumber()).as("usuarios entero").isTrue();
            assertThat(leido.path("usuarios").asInt()).isEqualTo(3);
        }

        @Test
        @DisplayName("y un decimal de verdad se conserva como decimal: normalizar no puede inventarse un entero")
        void un_decimal_de_verdad_no_se_convierte() {
            Document conFraccion = Document.mapBuilder().putBoolean("understood", true)
                    .putBoolean("out_of_domain", false).putList("necesarios", List.of())
                    .putList("recomendados", List.of())
                    .putNumber("usuarios", new java.math.BigDecimal("3.5")).putNumber("sedes", 1)
                    .putNumber("cajas", 0).build();
            when(cliente.converse(any(ConverseRequest.class)))
                    .thenReturn(conHerramienta(conFraccion, StopReason.TOOL_USE));

            JsonNode leido = leer(invocador.invoke(PROMPT).rawJson());

            assertThat(leido.path("usuarios").isIntegralNumber()).isFalse();
            assertThat(leido.path("usuarios").asText()).isEqualTo("3.5");
        }

        @Test
        @DisplayName("EL QUE IMPORTA: una respuesta en prosa NO pasa por buena, ni se le rescata un JSON de dentro")
        void la_prosa_no_pasa_por_buena() {
            when(cliente.converse(any(ConverseRequest.class))).thenReturn(respuesta(
                    "Claro, con mucho gusto. Para tu clinica te recomiendo el modulo CORE {} y"
                            + " tambien la historia clinica.",
                    3200, 900, StopReason.END_TURN));

            ModelInvocation invocacion = invocador.invoke(PROMPT);

            // Ni parsea...
            assertThat(leer(invocacion.rawJson())).as("la prosa no puede leerse como objeto JSON")
                    .isNull();
            // ...ni se cuela como un objeto vacio por haberle recortado las llaves,
            // que es lo que pasaria con un "de la primera { a la ultima }": eso seria
            // un understood=false valido, con propuesta y con 200.
            assertThat(invocacion.rawJson()).contains("Claro, con mucho gusto");
        }

        @Test
        @DisplayName("y el proveedor que no honra la herramienta tiene un desenlace DECLARADO y sistemico, no una excepcion cruda")
        void el_proveedor_que_no_la_honra() {
            when(cliente.converse(any(ConverseRequest.class)))
                    .thenReturn(respuesta("te recomiendo CORE", 3200, 900, StopReason.END_TURN));

            ModelInvocation invocacion = invocador.invoke(PROMPT);

            // No lanza: lanzar tiraria los contadores de una llamada ya cobrada.
            assertThat(invocacion.inputTokens()).isEqualTo(3200);
            assertThat(invocacion.failureCode()).isEqualTo("MODEL_STRUCTURED_OUTPUT_UNSUPPORTED");
            // Y es SISTEMICO: sin rama propia caeria en MODEL_OUTPUT_UNREADABLE, que
            // es aislado y va con WARN, y un cambio de familia de modelo se veria
            // igual que un prospecto que escribio raro -tres semanas mirando un panel-.
            assertThat(AiErrorType.deFailureCode(invocacion.failureCode()).esSistemico()).isTrue();
            assertThat(logs.list)
                    .anyMatch(evento -> evento.getFormattedMessage().contains("structured-output"));
        }

        @Test
        @DisplayName("cortada por longitud NO es averia del mecanismo: ahi el modelo si empezo a llamar a la herramienta")
        void cortada_por_longitud_sigue_siendo_aislada() {
            when(cliente.converse(any(ConverseRequest.class))).thenReturn(
                    respuesta("{\"understood\": tru", 3200, 900, StopReason.MAX_TOKENS));

            ModelInvocation invocacion = invocador.invoke(PROMPT);

            // Se cura subiendo max-output-tokens, no cambiando de modelo: mezclarlo
            // con el otro caso haria sonar una alerta sistemica por una respuesta
            // larga.
            assertThat(invocacion.failureCode()).isNull();
            assertThat(invocacion.stopReason()).isEqualTo("max_tokens");
        }
    }

    /**
     * <b>El uso de herramientas es una capacidad POR MODELO, no de la API.</b>
     * {@code Converse} unifica la interfaz de Bedrock, no lo que sabe hacer cada
     * familia: atarse a la via mas fuerte sin salida convertiria un cambio de
     * modelo en una rotura total en vez de una degradacion.
     */
    @Nested
    @DisplayName("El mecanismo es configuracion: se puede cambiar de familia de modelo")
    class Mecanismo {

        @Test
        @DisplayName("TOOL manda el esquema SIN strict: para un modelo con herramientas que rechaza el esquema estricto")
        void el_escalon_intermedio() {
            BedrockModelInvoker sinStrict = new BedrockModelInvoker(cliente, MODEL_ID, MAX_TOKENS,
                    StructuredOutputMode.TOOL, false);
            when(cliente.converse(any(ConverseRequest.class)))
                    .thenReturn(conHerramienta(entradaCompleta(), StopReason.TOOL_USE));

            sinStrict.invoke(PROMPT);

            ConverseRequest peticion = capturada();
            assertThat(peticion.toolConfig()).isNotNull();
            assertThat(peticion.toolConfig().tools().getFirst().toolSpec().strict()).isNull();
        }

        @Test
        @DisplayName("PROMPT no manda toolConfig NINGUNO y lee el texto: es la unica via que funciona en cualquier modelo")
        void el_escalon_universal() {
            BedrockModelInvoker porInstruccion = new BedrockModelInvoker(cliente, MODEL_ID,
                    MAX_TOKENS, StructuredOutputMode.PROMPT, false);
            when(cliente.converse(any(ConverseRequest.class))).thenReturn(respuesta(
                    "{\"understood\": true, \"necesarios\": []}", 10, 20, StopReason.END_TURN));

            ModelInvocation invocacion = porInstruccion.invoke(PROMPT);

            // Sin esto, un modelo sin herramientas no puede servir la feature en
            // absoluto y cambiar de familia exigiria un despliegue de codigo.
            assertThat(capturada().toolConfig()).isNull();
            assertThat(invocacion.failureCode()).isNull();
            assertThat(leer(invocacion.rawJson()).path("understood").asBoolean()).isTrue();
        }

        @Test
        @DisplayName("PROMPT quita el envoltorio de bloque de codigo: tres caracteres de adorno tumbarian la feature entera")
        void el_envoltorio_de_bloque_de_codigo() {
            BedrockModelInvoker porInstruccion = new BedrockModelInvoker(cliente, MODEL_ID,
                    MAX_TOKENS, StructuredOutputMode.PROMPT, false);
            when(cliente.converse(any(ConverseRequest.class))).thenReturn(
                    respuesta("```json\n{\"understood\": true}\n```", 10, 20, StopReason.END_TURN));

            JsonNode leido = leer(porInstruccion.invoke(PROMPT).rawJson());

            assertThat(leido).isNotNull();
            assertThat(leido.path("understood").asBoolean()).isTrue();
        }

        @Test
        @DisplayName("pero PROMPT tampoco rescata un JSON de dentro de la prosa: eso haria pasar «no se {} nada» como objeto vacio")
        void ni_siquiera_en_prompt_se_rescata_de_la_prosa() {
            BedrockModelInvoker porInstruccion = new BedrockModelInvoker(cliente, MODEL_ID,
                    MAX_TOKENS, StructuredOutputMode.PROMPT, false);
            when(cliente.converse(any(ConverseRequest.class))).thenReturn(
                    respuesta("No estoy seguro {} de lo que pides", 10, 20, StopReason.END_TURN));

            assertThat(leer(porInstruccion.invoke(PROMPT).rawJson())).isNull();
        }

        @Test
        @DisplayName("un modo mal escrito revienta el arranque con el valor dentro: caer al defecto en silencio es el fallo prohibido")
        void un_modo_mal_escrito_no_cae_al_defecto() {
            assertThatThrownBy(() -> StructuredOutputMode.of("TOOLS"))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("TOOLS")
                    .hasMessageContaining("TOOL_STRICT");
            assertThat(StructuredOutputMode.of("  prompt  "))
                    .isEqualTo(StructuredOutputMode.PROMPT);
        }
    }

    @Nested
    @DisplayName("El motivo de parada no puede reventar el INSERT de una llamada ya cobrada")
    class MotivoDeParada {

        @Test
        @DisplayName("un valor mas largo que la columna se ACOTA aqui: rechazarlo costaria la llamada que ya se pago")
        void se_acota_a_lo_que_cabe_en_la_columna() {
            when(cliente.converse(any(ConverseRequest.class)))
                    .thenReturn(ConverseResponse.builder()
                            .output(ConverseOutput.fromMessage(Message.builder()
                                    .role(ConversationRole.ASSISTANT)
                                    .content(ContentBlock.fromToolUse(ToolUseBlock.builder()
                                            .toolUseId("t").name(ProposalOutputSchema.HERRAMIENTA)
                                            .input(entradaCompleta()).build()))
                                    .build()))
                            .usage(TokenUsage.builder().inputTokens(1).outputTokens(2)
                                    .totalTokens(3).build())
                            .stopReason("model_context_window_exceeded_by_a_new_provider_value")
                            .build());

            ModelInvocation invocacion = invocador.invoke(PROMPT);

            assertThat(invocacion.stopReason()).hasSize(30)
                    .isEqualTo("model_context_window_exceeded_");
            // Y el turno sigue siendo escribible: es lo unico que esta guarda compra.
            assertThat(invocacion.failureCode()).isNull();
            assertThat(logs.list).anyMatch(evento -> evento.getFormattedMessage()
                    .contains("ai_proposal_turns.stop_reason"));
        }

        @Test
        @DisplayName("el andamiaje no miente: el valor mas largo que declara HOY el SDK son 29 caracteres, a uno del limite")
        void el_vocabulario_de_hoy_cabe_por_un_caracter() {
            assertThat(StopReason.MODEL_CONTEXT_WINDOW_EXCEEDED.toString()).hasSize(29);
            for (StopReason parada : StopReason.knownValues()) {
                assertThat(parada.toString()).as("motivo de parada %s", parada)
                        .hasSizeLessThanOrEqualTo(30);
            }
        }
    }

    // ── Andamiaje ──────────────────────────────────────────────────────────────

    /** La peticion que de verdad se mando al SDK. */
    private ConverseRequest capturada() {
        ArgumentCaptor<ConverseRequest> captor = ArgumentCaptor.forClass(ConverseRequest.class);
        org.mockito.Mockito.verify(cliente).converse(captor.capture());
        return captor.getValue();
    }

    /**
     * {@code null} si el cuerpo no es un objeto JSON, igual que hace el generador.
     */
    private static JsonNode leer(String rawJson) {
        if (rawJson == null || rawJson.isBlank())
            return null;
        try {
            JsonNode raiz = JACKSON.readTree(rawJson);
            return raiz.isObject() ? raiz : null;
        } catch (com.fasterxml.jackson.core.JacksonException ilegible) {
            return null;
        }
    }

    private static Set<String> nombres(Document lista) {
        return lista.asList().stream().map(Document::asString)
                .collect(java.util.stream.Collectors.toSet());
    }

    /** La entrada de la herramienta tal y como la emitiria un modelo que cumple. */
    private static Document entradaCompleta() {
        return Document.mapBuilder().putBoolean("understood", true)
                .putBoolean("out_of_domain", false)
                .putList("necesarios",
                        List.of(Document.mapBuilder().putString("code", "CORE")
                                .putString("motivo", "Es la base y va contigo").build()))
                .putList("recomendados",
                        List.of(Document.mapBuilder().putString("code", "SCHEDULING")
                                .putString("motivo", "Porque agendas citas").build()))
                .putNumber("usuarios", 3).putNumber("sedes", 1).putNumber("cajas", 0).build();
    }

    @Nested
    @DisplayName("La conversacion con el modelo, escrita en el log")
    class LogDeLaConversacion {

        @Test
        @DisplayName("apagado por defecto: no escribe ni el prompt ni la respuesta")
        void apagado_no_escribe_nada() {
            when(cliente.converse(any(ConverseRequest.class)))
                    .thenReturn(respuesta("{\"understood\":true}", 10, 20, StopReason.END_TURN));

            invocador.invoke(PROMPT);

            assertThat(lineasDelPayload()).isEmpty();
        }

        @Test
        @DisplayName("encendido: escribe lo que se manda y lo que contesta, enteros")
        void encendido_escribe_las_dos_mitades() {
            BedrockModelInvoker conLog = new BedrockModelInvoker(cliente, MODEL_ID, MAX_TOKENS,
                    MODO, true);
            when(cliente.converse(any(ConverseRequest.class)))
                    .thenReturn(respuesta("{\"understood\":true}", 3200, 900, StopReason.END_TURN));

            conLog.invoke(PROMPT);

            assertThat(lineasDelPayload()).hasSize(2);
            assertThat(lineasDelPayload().getFirst()).contains(PROMPT.system())
                    .contains(PROMPT.user()).contains(MODEL_ID);
            assertThat(lineasDelPayload().get(1)).contains("{\"understood\":true}")
                    .contains("end_turn").contains("3200").contains("900");
        }

        /**
         * &#9940; El prompt se escribe ANTES de invocar, y esta prueba es la razon: si
         * el SDK revienta o se agota el timeout, lo que se mando es lo unico que
         * explica por que. Escribirlo despues lo perderia justo en el caso que se esta
         * depurando.
         */
        @Test
        @DisplayName("si la invocacion falla, lo que se mando ya quedo escrito")
        void la_peticion_sobrevive_al_fallo() {
            BedrockModelInvoker conLog = new BedrockModelInvoker(cliente, MODEL_ID, MAX_TOKENS,
                    MODO, true);
            when(cliente.converse(any(ConverseRequest.class)))
                    .thenThrow(SdkClientException.create("boom"));

            assertThatThrownBy(() -> conLog.invoke(PROMPT))
                    .isInstanceOf(ModelInvocationException.class);

            assertThat(lineasDelPayload()).hasSize(1);
            assertThat(lineasDelPayload().getFirst()).contains(PROMPT.user());
        }

        /**
         * Filtrado por NOMBRE de logger y no por contenido: el canal tiene que ser
         * {@code AI_PAYLOAD} y solo ese, porque es lo que permite a
         * {@code logback-spring.xml} enrutarlo aparte -sin redactar en local, por la
         * raiz redactada en dev-. Si alguien lo escribiera por el logger de la clase,
         * estas aserciones seguirian pasando por contenido y el enrutado seria mentira.
         *
         * <p>
         * El sumidero es el del propio canal y no el de la raiz. El porque esta en
         * {@code montar()}, y es la diferencia entre medir y creer que se mide.
         */
        private java.util.List<String> lineasDelPayload() {
            return delPayload.list.stream()
                    .filter(evento -> "AI_PAYLOAD".equals(evento.getLoggerName()))
                    .map(ILoggingEvent::getFormattedMessage).toList();
        }

        /**
         * &#9940; <strong>El andamiaje no da falsos verdes.</strong> Las tres pruebas
         * de arriba se apoyan en que el sumidero VE lo que el canal escribe; si dejara
         * de verlo -exactamente lo que pasaba leyendo desde la raiz-,
         * {@code apagado_no_escribe_nada} seguiria verde y las otras dos dirian "la
         * funcion no escribe nada" sobre una implementacion intacta. Esto separa las
         * dos lecturas.
         */
        @Test
        @DisplayName("el sumidero del canal ve lo que el canal escribe, este quien este"
                + " enganchado a la raiz")
        void el_sumidero_del_canal_no_depende_de_la_raiz() {
            org.slf4j.LoggerFactory.getLogger("AI_PAYLOAD").info("sonda del andamiaje");

            assertThat(lineasDelPayload()).containsExactly("sonda del andamiaje");
        }
    }

    private static ConverseResponse conHerramienta(Document entrada, StopReason parada) {
        return ConverseResponse.builder()
                .output(ConverseOutput.fromMessage(Message.builder()
                        .role(ConversationRole.ASSISTANT)
                        .content(ContentBlock.fromToolUse(ToolUseBlock.builder().toolUseId("tu-1")
                                .name(ProposalOutputSchema.HERRAMIENTA).input(entrada).build()))
                        .build()))
                .usage(TokenUsage.builder().inputTokens(3200).outputTokens(900).totalTokens(4100)
                        .build())
                .stopReason(parada).build();
    }

    private static ConverseResponse respuesta(String texto, int entrada, int salida,
            StopReason parada) {
        return ConverseResponse.builder().output(ConverseOutput.fromMessage(Message.builder()
                .role(ConversationRole.ASSISTANT).content(ContentBlock.fromText(texto)).build()))
                .usage(TokenUsage.builder().inputTokens(entrada).outputTokens(salida)
                        .totalTokens(entrada + salida).build())
                .stopReason(parada).build();
    }

    /**
     * Un {@link AwsServiceException} generico con el estado dado: es lo que llega
     * cuando el fallo no tiene una clase propia en el modelo del SDK —credenciales
     * caducadas, firma invalida, cliente no reconocido—.
     */
    private static AwsServiceException porEstado(int estado) {
        // OJO: .statusCode(...) va en el builder de la EXCEPCION. statusCode() es un
        // campo propio de SdkServiceException (sdk-core, SdkServiceException:71), no
        // se deriva de awsErrorDetails.sdkHttpResponse(). Rellenar solo la respuesta
        // HTTP deja statusCode() en 0, y entonces este doble prueba la rama de "sin
        // clasificar" creyendo que prueba la de 401 — que es como este mismo fixture
        // dio tres falsos rojos antes de corregirse.
        return AwsServiceException.builder().statusCode(estado)
                .awsErrorDetails(AwsErrorDetails.builder().errorMessage(MENSAJE_DEL_SDK)
                        .sdkHttpResponse(SdkHttpResponse.builder().statusCode(estado).build())
                        .build())
                .message(MENSAJE_DEL_SDK).build();
    }

    private static String cadena(Throwable error) {
        StringBuilder texto = new StringBuilder();
        for (Throwable actual = error; actual != null; actual = actual.getCause()) {
            texto.append(actual.getClass().getName()).append(' ').append(actual.getMessage())
                    .append(' ');
            if (actual.getCause() == actual)
                break;
        }
        return texto.toString();
    }
}
