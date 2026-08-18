package com.vetsoftware.app.infrastructure.logging;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Cubre el comportamiento del decorador que no pasa por el pipeline completo de
 * Logback ({@code LogRedactionPipelineTest} y
 * {@code AuditFieldsSurviveRedactionTest} ya cubren {@code append(...)} de
 * extremo a extremo): arranque sin appenders anidados, parada idempotente y la
 * gestión de appenders por nombre e instancia.
 */
class RedactingAppenderTest {

    private final LoggerContext context = new LoggerContext();

    @Test
    @DisplayName("advierte si arranca sin ningún appender anidado; sus eventos se descartarían")
    void warns_when_started_without_any_nested_appender() {
        RedactingAppender appender = new RedactingAppender();
        appender.setContext(context);
        appender.setName("EMPTY");

        appender.start();

        assertThat(context.getStatusManager().getCopyOfStatusList()).extracting(Object::toString)
                .anyMatch(message -> message.contains("No hay appenders anidados"));
    }

    @Test
    @DisplayName("no advierte cuando arranca con al menos un appender anidado")
    void does_not_warn_when_started_with_a_nested_appender() {
        RedactingAppender appender = new RedactingAppender();
        appender.setContext(context);
        appender.setName("WITH_NESTED");
        appender.addAppender(namedListAppender("NESTED"));

        appender.start();

        assertThat(context.getStatusManager().getCopyOfStatusList()).extracting(Object::toString)
                .noneMatch(message -> message.contains("No hay appenders anidados"));
    }

    @Test
    @DisplayName("detener un appender que nunca arrancó no falla y no cambia su estado")
    void stop_is_idempotent_when_never_started() {
        RedactingAppender appender = new RedactingAppender();
        appender.setContext(context);

        appender.stop();

        assertThat(appender.isStarted()).isFalse();
    }

    @Test
    @DisplayName("expone y desconecta appenders anidados por nombre e instancia")
    void manages_nested_appenders_by_name_and_instance() {
        RedactingAppender appender = new RedactingAppender();
        appender.setContext(context);
        ListAppender<ILoggingEvent> first = namedListAppender("FIRST");
        ListAppender<ILoggingEvent> second = namedListAppender("SECOND");
        appender.addAppender(first);
        appender.addAppender(second);

        assertThat(appender.getAppender("FIRST")).isSameAs(first);
        assertThat(appender.isAttached(second)).isTrue();
        boolean detachedByInstance = appender.detachAppender(first);
        boolean detachedByName = appender.detachAppender("SECOND");

        assertThat(detachedByInstance).isTrue();
        assertThat(detachedByName).isTrue();
        assertThat(appender.iteratorForAppenders().hasNext()).isFalse();
    }

    @Test
    @DisplayName("stop() detiene y desconecta todos los appenders anidados cuando estaba iniciado")
    void stop_detaches_and_stops_all_nested_appenders_when_started() {
        RedactingAppender appender = new RedactingAppender();
        appender.setContext(context);
        ListAppender<ILoggingEvent> nested = namedListAppender("NESTED");
        appender.addAppender(nested);
        nested.start();
        appender.start();

        appender.stop();

        assertThat(nested.isStarted()).isFalse();
        assertThat(appender.iteratorForAppenders().hasNext()).isFalse();
    }

    private ListAppender<ILoggingEvent> namedListAppender(String name) {
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.setContext(context);
        listAppender.setName(name);
        return listAppender;
    }
}
