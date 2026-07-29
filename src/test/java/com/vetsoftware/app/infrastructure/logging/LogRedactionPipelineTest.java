package com.vetsoftware.app.infrastructure.logging;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.ThrowableProxy;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;
import ch.qos.logback.core.read.ListAppender;
import java.io.IOException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.slf4j.event.KeyValuePair;

/**
 * Prueba de extremo a extremo de la política de redacción (OBS-019) sobre un pipeline de Logback
 * <b>real</b>: {@link RedactingAppender} envolviendo un appender destino, igual que en
 * {@code logback-spring.xml}.
 *
 * <p>El valor señuelo se inyecta por las cuatro superficies posibles (mensaje formateado, MDC, pares
 * clave-valor y cadena de excepciones) y se afirma que no sobrevive en <em>ninguna</em> de ellas en el
 * evento que recibe el appender destino — que es exactamente lo que se serializa a Loki.
 */
class LogRedactionPipelineTest {

    private static final String DECOY = "SENUELO-No-Debe-Salir-7f3a";

    private LoggerContext context;
    private ListAppender<ILoggingEvent> sink;
    private Logger logger;

    @BeforeEach
    void wirePipeline() {
        context = new LoggerContext();
        // Un LoggerContext construido a mano no trae adaptador de MDC (en producción lo inyecta el
        // binding de SLF4J), y LoggingEvent.getMDCPropertyMap() reventaría con NPE. Se reutiliza el
        // adaptador real para que los MDC.put de la prueba lleguen de verdad al evento.
        context.setMDCAdapter(MDC.getMDCAdapter());
        context.start();

        sink = new ListAppender<>();
        sink.setContext(context);
        sink.setName("SINK");
        sink.start();

        RedactingAppender redacting = new RedactingAppender();
        redacting.setContext(context);
        redacting.setName("REDACTED_SINK");
        redacting.addAppender(sink);
        redacting.start();

        logger = context.getLogger("redaction-test");
        logger.setLevel(Level.INFO);
        logger.addAppender(redacting);
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
        context.stop();
    }

    private ILoggingEvent onlyEvent() {
        assertThat(sink.list).hasSize(1);
        return sink.list.get(0);
    }

    /** Todo el texto que el evento puede llegar a serializar, en una sola cadena. */
    private static String everythingSerialized(ILoggingEvent event) {
        StringBuilder all = new StringBuilder();
        all.append(event.getFormattedMessage()).append('\n');
        all.append(event.getMessage()).append('\n');
        if (event.getArgumentArray() != null) {
            for (Object argument : event.getArgumentArray()) {
                all.append(argument).append('\n');
            }
        }
        event.getMDCPropertyMap().forEach((key, value) ->
                all.append(key).append('=').append(value).append('\n'));
        if (event.getKeyValuePairs() != null) {
            for (KeyValuePair pair : event.getKeyValuePairs()) {
                all.append(pair.key).append('=').append(pair.value).append('\n');
            }
        }
        if (event.getThrowableProxy() != null) {
            all.append(ThrowableProxyUtil.asString(event.getThrowableProxy()));
        }
        return all.toString();
    }

    // -------------------------------------------------------------------------------------------
    // Las cuatro superficies
    // -------------------------------------------------------------------------------------------

    @Test
    @DisplayName("un secreto interpolado como argumento no llega al appender destino")
    void redactsSecretsComposedFromTemplateAndArgument() {
        // El argumento aislado es texto anodino; el secreto solo existe al unir plantilla y argumento.
        // Es la razón por la que se redacta el mensaje ya formateado y no argumento a argumento.
        logger.info("password={}", DECOY);

        ILoggingEvent event = onlyEvent();
        assertThat(everythingSerialized(event)).doesNotContain(DECOY);
        assertThat(event.getFormattedMessage()).isEqualTo("password=***");
    }

    @Test
    @DisplayName("el argumento original no queda accesible para que un encoder reformatee el mensaje")
    void doesNotExposeRawArgumentsForReformatting() {
        logger.info("password={}", DECOY);

        ILoggingEvent event = onlyEvent();
        assertThat(event.getArgumentArray()).isNull();
        assertThat(event.getMessage()).isEqualTo("password=***");
    }

    @Test
    void redactsMdcValuesOutsideTheAllowlist() {
        MDC.put("owner.email", DECOY + "@gmail.com");
        MDC.put(MdcKeys.ACTOR_EMPLOYEE_ID, "77");

        logger.info("actualizando propietario");

        ILoggingEvent event = onlyEvent();
        assertThat(everythingSerialized(event)).doesNotContain(DECOY);
        assertThat(event.getMDCPropertyMap())
                .containsEntry("owner.email", LogRedactor.MASK)
                .containsEntry(MdcKeys.ACTOR_EMPLOYEE_ID, "77");
    }

    @Test
    void redactsStructuredKeyValuePairsOutsideTheAllowlist() {
        logger.atInfo()
                .addKeyValue("event", "login_success")
                .addKeyValue("owner.diagnosis", DECOY)
                .log("evento de auditoría");

        ILoggingEvent event = onlyEvent();
        assertThat(everythingSerialized(event)).doesNotContain(DECOY);
        assertThat(event.getKeyValuePairs())
                .extracting(pair -> pair.key + "=" + pair.value)
                .containsExactly("event=login_success", "owner.diagnosis=" + LogRedactor.MASK);
    }

    @Test
    @DisplayName("el mensaje de una excepción y de toda su cadena de causas se redacta")
    void redactsThrowableChainMessages() {
        IOException root = new IOException("fallo al escribir token=" + DECOY);
        IllegalStateException wrapper = new IllegalStateException("password=" + DECOY, root);
        wrapper.addSuppressed(new IllegalArgumentException("clave=" + DECOY));

        logger.error("Unexpected error", wrapper);

        ILoggingEvent event = onlyEvent();
        assertThat(everythingSerialized(event)).doesNotContain(DECOY);

        IThrowableProxy proxy = event.getThrowableProxy();
        assertThat(proxy.getMessage()).isEqualTo("password=***");
        assertThat(proxy.getCause().getMessage()).isEqualTo("fallo al escribir token=***");
        assertThat(proxy.getSuppressed()[0].getMessage()).isEqualTo("clave=***");
    }

    @Test
    @DisplayName("al redactar una excepción se conservan el tipo original y el stacktrace")
    void preservesThrowableTypeAndStackTraceWhenRedacting() {
        logger.error("Unexpected error", new IllegalStateException("password=" + DECOY));

        IThrowableProxy proxy = onlyEvent().getThrowableProxy();
        String rendered = ThrowableProxyUtil.asString(proxy);

        assertThat(rendered).contains(IllegalStateException.class.getName());
        assertThat(proxy.getStackTraceElementProxyArray()).isNotEmpty();
    }

    @Test
    @DisplayName("una excepción sin datos sensibles llega intacta: no se paga fidelidad sin motivo")
    void leavesCleanThrowablesCompletelyUntouched() {
        IllegalStateException clean = new IllegalStateException("estado inválido de la sesión de caja");

        logger.error("Unexpected error", clean);

        IThrowableProxy proxy = onlyEvent().getThrowableProxy();
        assertThat(proxy).isInstanceOf(ThrowableProxy.class);
        assertThat(((ThrowableProxy) proxy).getThrowable()).isSameAs(clean);
        assertThat(proxy.getClassName()).isEqualTo(IllegalStateException.class.getName());
    }

    // -------------------------------------------------------------------------------------------
    // Comportamiento del decorador
    // -------------------------------------------------------------------------------------------

    @Test
    @DisplayName("un evento limpio se reenvía sin copiarse")
    void forwardsCleanEventsWithoutCopying() {
        logger.info("mutation POST /api/v1/owners -> 201 (SUCCESS)");

        assertThat(onlyEvent()).isNotInstanceOf(RedactedLoggingEvent.class);
    }

    @Test
    void forwardsToEveryNestedAppender() {
        ListAppender<ILoggingEvent> second = new ListAppender<>();
        second.setContext(context);
        second.setName("SECOND");
        second.start();
        ((RedactingAppender) logger.getAppender("REDACTED_SINK")).addAppender(second);

        logger.info("password={}", DECOY);

        assertThat(second.list).hasSize(1);
        assertThat(second.list.get(0).getFormattedMessage()).isEqualTo("password=***");
        assertThat(sink.list).hasSize(1);
    }

    @Test
    @DisplayName("preserva el nivel, el logger y la marca de tiempo del evento original")
    void preservesEventIdentity() {
        long before = System.currentTimeMillis();

        logger.warn("password={}", DECOY);

        ILoggingEvent event = onlyEvent();
        assertThat(event.getLevel()).isEqualTo(Level.WARN);
        assertThat(event.getLoggerName()).isEqualTo("redaction-test");
        assertThat(event.getThreadName()).isEqualTo(Thread.currentThread().getName());
        assertThat(event.getTimeStamp()).isGreaterThanOrEqualTo(before);
    }
}
