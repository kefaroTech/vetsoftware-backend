package com.vetsoftware.app.infrastructure.logging;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.pattern.ThrowableProxyConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.boot.logging.logback.StructuredLogEncoder;
import org.springframework.core.env.Environment;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;

/**
 * Prueba del formato JSON de consola sobre el pipeline <b>real</b> de Logback:
 * logger → {@link RedactingAppender} → evento redactado →
 * {@link StructuredConsoleLogFormatter}. Es exactamente la cadena que corre en
 * dev y prod, donde el destino final es stdout y de ahí CloudWatch.
 *
 * <p>
 * El riesgo que cubre no es cosmético. Al pasar de texto plano a JSON entra en
 * juego un <em>encoder</em> que reformatea el evento, y un encoder que leyera
 * {@code getArgumentArray()} en vez del mensaje ya formateado reconstruiría el
 * mensaje <b>sin redactar</b>, deshaciendo {@link LogRedactor} en el último
 * metro. Por eso el señuelo se inyecta por las tres superficies que el JSON
 * serializa —mensaje parametrizado, MDC y excepción— y se afirma que no
 * sobrevive en la línea emitida.
 *
 * @see LogRedactionPipelineTest
 */
class StructuredConsoleLogFormatterTest {

    private static final String DECOY = "SENUELO-No-Debe-Salir-7f3a";

    /**
     * Derived field del datasource de Loki que enlaza a Tempo. Está copiado tal
     * cual del stack: si el nombre del campo deja de casar con él, el salto de log
     * a traza se rompe en silencio y nadie se entera hasta que hace falta.
     */
    private static final Pattern LOKI_TRACE_DERIVED_FIELD = Pattern
            .compile("[tT]race_?[iI][dD]\"?[:=]\"?(\\w+)");

    private static final ObjectMapper JSON = new ObjectMapper();

    private LoggerContext context;
    private ListAppender<ILoggingEvent> sink;
    private ThrowableProxyConverter throwableProxyConverter;
    private StructuredConsoleLogFormatter formatter;
    private Logger logger;

    @BeforeEach
    void wirePipeline() {
        context = new LoggerContext();
        // Un LoggerContext construido a mano no trae adaptador de MDC; se reutiliza el
        // real para que los MDC.put de la prueba lleguen de verdad al evento.
        context.setMDCAdapter(MDC.getMDCAdapter());
        context.start();

        throwableProxyConverter = new ThrowableProxyConverter();
        throwableProxyConverter.setContext(context);
        throwableProxyConverter.start();
        formatter = new StructuredConsoleLogFormatter(throwableProxyConverter);

        sink = new ListAppender<>();
        sink.setContext(context);
        sink.setName("SINK");
        sink.start();

        RedactingAppender redacting = new RedactingAppender();
        redacting.setContext(context);
        redacting.setName("REDACTED_JSON_CONSOLE");
        redacting.addAppender(sink);
        redacting.start();

        logger = context.getLogger("json-console-test");
        logger.setLevel(Level.INFO);
        logger.addAppender(redacting);
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
        throwableProxyConverter.stop();
        context.stop();
    }

    private ILoggingEvent onlyEvent() {
        assertThat(sink.list).hasSize(1);
        return sink.list.get(0);
    }

    /**
     * La línea tal cual saldría por stdout: se usa {@code formatAsBytes}, que es
     * literalmente lo que invoca {@code StructuredLogEncoder.encode(...)}.
     */
    private String emittedLine() {
        return new String(formatter.formatAsBytes(onlyEvent(), StandardCharsets.UTF_8),
                StandardCharsets.UTF_8);
    }

    private JsonNode emittedJson() throws Exception {
        return JSON.readTree(emittedLine());
    }

    @Test
    @DisplayName("emite una sola línea JSON por evento, terminada en salto de línea")
    void emits_one_json_line_per_event() {
        logger.info("arranque completado");

        String line = emittedLine();

        assertThat(line).endsWith("\n");
        assertThat(line.stripTrailing()).doesNotContain("\n");
    }

    @Test
    @DisplayName("lleva timestamp, nivel, logger, hilo y mensaje")
    void carries_the_base_event_fields() throws Exception {
        logger.warn("disco al 91 por ciento");

        JsonNode json = emittedJson();

        assertThat(json.get("timestamp").asText()).endsWith("Z");
        assertThat(json.get("level").asText()).isEqualTo("WARN");
        assertThat(json.get("logger").asText()).isEqualTo("json-console-test");
        assertThat(json.get("thread").asText()).isEqualTo(Thread.currentThread().getName());
        assertThat(json.get("message").asText()).isEqualTo("disco al 91 por ciento");
    }

    @Test
    @DisplayName("trace_id y span_id salen con el nombre que enlaza con Tempo")
    void trace_and_span_use_the_name_the_loki_derived_field_expects() throws Exception {
        MDC.put("traceId", "4bf92f3577b34da6a3ce929d0e0e4736");
        MDC.put("spanId", "00f067aa0ba902b7");

        logger.info("peticion atendida");
        String line = emittedLine();
        JsonNode json = JSON.readTree(line);

        assertThat(json.get("trace_id").asText()).isEqualTo("4bf92f3577b34da6a3ce929d0e0e4736");
        assertThat(json.get("span_id").asText()).isEqualTo("00f067aa0ba902b7");
        // No se duplica con el nombre original de Micrometer.
        assertThat(json.has("traceId")).isFalse();
        assertThat(json.has("spanId")).isFalse();

        Matcher matcher = LOKI_TRACE_DERIVED_FIELD.matcher(line);
        assertThat(matcher.find()).as("el derived field de Loki ya no encuentra la traza").isTrue();
        assertThat(matcher.group(1)).isEqualTo("4bf92f3577b34da6a3ce929d0e0e4736");
    }

    @Test
    @DisplayName("todo el MDC viaja en el JSON, no solo la traza")
    void the_whole_mdc_travels() throws Exception {
        MDC.put(MdcKeys.ACTOR_TYPE, "EMPLOYEE");
        MDC.put(MdcKeys.ACTOR_EMPLOYEE_ID, "77");
        MDC.put(MdcKeys.ACTOR_COMPANY_ID, "3");
        MDC.put(MdcKeys.CLIENT_IP, "192.0.2.10");
        MDC.put(MdcKeys.HTTP_METHOD, "POST");
        MDC.put(MdcKeys.HTTP_PATH, "/api/v1/owners");

        logger.info("alta registrada");
        JsonNode json = emittedJson();

        assertThat(json.get(MdcKeys.ACTOR_TYPE).asText()).isEqualTo("EMPLOYEE");
        assertThat(json.get(MdcKeys.ACTOR_EMPLOYEE_ID).asText()).isEqualTo("77");
        assertThat(json.get(MdcKeys.ACTOR_COMPANY_ID).asText()).isEqualTo("3");
        assertThat(json.get(MdcKeys.CLIENT_IP).asText()).isEqualTo("192.0.2.10");
        assertThat(json.get(MdcKeys.HTTP_METHOD).asText()).isEqualTo("POST");
        assertThat(json.get(MdcKeys.HTTP_PATH).asText()).isEqualTo("/api/v1/owners");
    }

    @Test
    @DisplayName("los pares de addKeyValue viajan como campos del JSON")
    void structured_key_value_pairs_travel_as_json_fields() throws Exception {
        logger.atInfo().setMessage("evento de auditoria").addKeyValue("event", "LOGIN_OK")
                .addKeyValue("outcome", "SUCCESS").log();

        JsonNode json = emittedJson();

        assertThat(json.get("event").asText()).isEqualTo("LOGIN_OK");
        assertThat(json.get("outcome").asText()).isEqualTo("SUCCESS");
    }

    @Test
    @DisplayName("un secreto en el mensaje no aparece en claro en el JSON")
    void a_secret_in_the_message_never_reaches_the_json() throws Exception {
        logger.info("autenticando con password=" + DECOY);

        String line = emittedLine();

        assertThat(line).doesNotContain(DECOY);
        assertThat(JSON.readTree(line).get("message").asText())
                .isEqualTo("autenticando con password=" + LogRedactor.MASK);
    }

    @Test
    @DisplayName("el encoder no reconstruye el mensaje desde los argumentos sin redactar")
    void the_encoder_cannot_rebuild_the_message_from_the_raw_arguments() throws Exception {
        // El caso peligroso: el argumento aislado es anodino y solo el mensaje YA
        // formateado revela el secreto. Un encoder que leyera getArgumentArray()
        // volvería a emitir el valor limpio.
        logger.info("password={}", DECOY);

        ILoggingEvent redacted = onlyEvent();
        String line = emittedLine();

        assertThat(redacted.getArgumentArray())
                .as("RedactedLoggingEvent debe seguir ocultando los argumentos originales")
                .isNull();
        assertThat(line).doesNotContain(DECOY);
        assertThat(JSON.readTree(line).get("message").asText())
                .isEqualTo("password=" + LogRedactor.MASK);
    }

    @Test
    @DisplayName("un secreto en el MDC no aparece en claro en el JSON")
    void a_secret_in_the_mdc_never_reaches_the_json() throws Exception {
        // Clave no declarada en LogFieldPolicy: la allowlist la enmascara entera.
        MDC.put("session.secret", DECOY);

        logger.info("sesion abierta");
        String line = emittedLine();

        assertThat(line).doesNotContain(DECOY);
        assertThat(JSON.readTree(line).get("session.secret").asText()).isEqualTo(LogRedactor.MASK);
    }

    @Test
    @DisplayName("la excepción viaja con su stacktrace y con el mensaje redactado")
    void the_exception_travels_redacted_with_its_stack_trace() throws Exception {
        logger.error("fallo al conectar", new IOException("password=" + DECOY));

        String line = emittedLine();
        JsonNode exception = JSON.readTree(line).get("exception");

        assertThat(line).doesNotContain(DECOY);
        assertThat(exception.get("message").asText()).isEqualTo("password=" + LogRedactor.MASK);
        // El tipo original sobrevive en el stacktrace aunque el proxy se haya
        // sustituido por la copia redactada.
        assertThat(exception.get("stacktrace").asText()).contains("java.io.IOException")
                .contains("the_exception_travels_redacted_with_its_stack_trace");
    }

    @Test
    @DisplayName("sin excepción no se emite el objeto exception")
    void no_exception_member_when_the_event_has_no_throwable() throws Exception {
        logger.info("todo en orden");

        assertThat(emittedJson().has("exception")).isFalse();
    }

    /**
     * El encoder instancia el formato <b>por reflexión desde su nombre</b>, así que
     * ni el compilador ni el resto de esta prueba verían que el constructor dejó de
     * ser resoluble o que el argumento genérico ya no es {@code ILoggingEvent}: el
     * fallo aparecería al arrancar en dev, no aquí.
     */
    @Test
    @DisplayName("el encoder de Spring Boot resuelve este formato por su nombre de clase")
    void the_spring_boot_encoder_resolves_this_formatter_by_class_name() throws Exception {
        LoggerContext encoderContext = new LoggerContext();
        encoderContext.putObject(Environment.class.getName(), new StandardEnvironment());
        StructuredLogEncoder encoder = new StructuredLogEncoder();
        encoder.setContext(encoderContext);
        encoder.setFormat(StructuredConsoleLogFormatter.class.getName());

        encoder.start();
        logger.info("linea de arranque");
        String line = new String(encoder.encode(onlyEvent()), StandardCharsets.UTF_8);

        assertThat(encoder.isStarted()).isTrue();
        assertThat(JSON.readTree(line).get("message").asText()).isEqualTo("linea de arranque");
        encoder.stop();
    }

    @Test
    @DisplayName("logback-spring.xml referencia exactamente esta clase como formato")
    void the_logback_configuration_points_at_this_formatter() throws Exception {
        String configuration = new ClassPathResource("logback-spring.xml")
                .getContentAsString(StandardCharsets.UTF_8);

        assertThat(configuration)
                .as("renombrar el formato sin actualizar logback-spring.xml rompe el arranque")
                .contains("<format>" + StructuredConsoleLogFormatter.class.getName() + "</format>");
    }
}
