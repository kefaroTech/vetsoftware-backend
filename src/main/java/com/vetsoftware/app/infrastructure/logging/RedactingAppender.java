package com.vetsoftware.app.infrastructure.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.spi.AppenderAttachable;
import ch.qos.logback.core.spi.AppenderAttachableImpl;
import java.util.Iterator;

/**
 * Appender decorador: redacta cada evento y lo reenvía a los appenders anidados.
 *
 * <p>Es el punto único por el que pasa <b>todo</b> lo que sale del proceso. Se configura envolviendo
 * a los appenders reales en {@code logback-spring.xml}:
 *
 * <pre>{@code
 * <appender name="REDACTED_OTEL" class="com.vetsoftware.app.infrastructure.logging.RedactingAppender">
 *     <appender-ref ref="OTEL"/>
 * </appender>
 * }</pre>
 *
 * <p><b>Por qué un decorador y no un filtro:</b> los {@code Filter}/{@code TurboFilter} de Logback
 * solo deciden si un evento pasa o no — no pueden transformarlo. Un decorador sí, y además cubre
 * cualquier appender presente o futuro sin tocarlo, así que no hay forma de añadir un destino nuevo
 * y olvidarse de la redacción: si no se envuelve, no recibe eventos.
 *
 * <p><b>Compatibilidad con OpenTelemetry:</b> {@code OpenTelemetryAppender.install(...)} recorre los
 * appenders del contexto y desciende por los que implementan {@link AppenderAttachable}, así que el
 * appender de OTel anidado aquí se sigue conectando al {@code SdkLoggerProvider} con normalidad.
 *
 * <p>Los {@code <appender>} envueltos deben declararse antes de este en el XML y como hijos directos
 * de {@code <configuration>}: la regla de Joran para {@code appender-ref} anidado es
 * {@code configuration/appender/appender-ref}, y no casa dentro de un {@code <springProfile>}. El
 * gating por perfil se hace en {@code <root>}, referenciando este appender.
 *
 * @see LogRedactor
 * @see RedactedLoggingEvent
 */
public final class RedactingAppender extends AppenderBase<ILoggingEvent>
        implements AppenderAttachable<ILoggingEvent> {

    private final AppenderAttachableImpl<ILoggingEvent> nested = new AppenderAttachableImpl<>();

    @Override
    public void start() {
        if (!nested.iteratorForAppenders().hasNext()) {
            addWarn("No hay appenders anidados en [" + name + "]; sus eventos se descartan.");
        }
        super.start();
    }

    @Override
    protected void append(ILoggingEvent event) {
        nested.appendLoopOnAppenders(RedactedLoggingEvent.of(event));
    }

    @Override
    public void stop() {
        if (!isStarted()) {
            return;
        }
        super.stop();
        nested.detachAndStopAllAppenders();
    }

    // --- AppenderAttachable ---

    @Override
    public void addAppender(Appender<ILoggingEvent> newAppender) {
        nested.addAppender(newAppender);
    }

    @Override
    public Iterator<Appender<ILoggingEvent>> iteratorForAppenders() {
        return nested.iteratorForAppenders();
    }

    @Override
    public Appender<ILoggingEvent> getAppender(String appenderName) {
        return nested.getAppender(appenderName);
    }

    @Override
    public boolean isAttached(Appender<ILoggingEvent> appender) {
        return nested.isAttached(appender);
    }

    @Override
    public void detachAndStopAllAppenders() {
        nested.detachAndStopAllAppenders();
    }

    @Override
    public boolean detachAppender(Appender<ILoggingEvent> appender) {
        return nested.detachAppender(appender);
    }

    @Override
    public boolean detachAppender(String appenderName) {
        return nested.detachAppender(appenderName);
    }
}
