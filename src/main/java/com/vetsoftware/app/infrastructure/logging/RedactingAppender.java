package com.vetsoftware.app.infrastructure.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import ch.qos.logback.core.spi.AppenderAttachable;
import ch.qos.logback.core.spi.AppenderAttachableImpl;
import ch.qos.logback.core.util.ReentryGuard;
import ch.qos.logback.core.util.ReentryGuardFactory;
import java.util.Iterator;

/**
 * Appender decorador: redacta cada evento y lo reenvía a los appenders
 * anidados.
 *
 * <p>
 * Es el punto único por el que pasa <b>todo</b> lo que sale del proceso. Se
 * configura envolviendo a los appenders reales en {@code logback-spring.xml}:
 *
 * <pre>{@code
 * <appender name="REDACTED_OTEL" class=
"com.vetsoftware.app.infrastructure.logging.RedactingAppender">
 *     <appender-ref ref="OTEL"/>
 * </appender>
 * }</pre>
 *
 * <p>
 * <b>Por qué un decorador y no un filtro:</b> los
 * {@code Filter}/{@code TurboFilter} de Logback solo deciden si un evento pasa
 * o no — no pueden transformarlo. Un decorador sí, y además cubre cualquier
 * appender presente o futuro sin tocarlo, así que no hay forma de añadir un
 * destino nuevo y olvidarse de la redacción: si no se envuelve, no recibe
 * eventos.
 *
 * <p>
 * <b>Compatibilidad con OpenTelemetry:</b>
 * {@code OpenTelemetryAppender.install(...)} recorre los appenders del contexto
 * y desciende por los que implementan {@link AppenderAttachable}, así que el
 * appender de OTel anidado aquí se sigue conectando al
 * {@code SdkLoggerProvider} con normalidad.
 *
 * <p>
 * Los {@code <appender>} envueltos deben declararse antes de este en el XML y
 * como hijos directos de {@code <configuration>}: la regla de Joran para
 * {@code appender-ref} anidado es {@code configuration/appender/appender-ref},
 * y no casa dentro de un {@code <springProfile>}. El gating por perfil se hace
 * en {@code <root>}, referenciando este appender.
 *
 * <p>
 * <b>Por qué {@link UnsynchronizedAppenderBase} y no {@code AppenderBase}
 * (incidencia #92):</b> {@code AppenderBase.doAppend} es {@code synchronized}
 * sobre la instancia del appender. Como todo lo que emite el proceso atraviesa
 * este decorador, cada hilo de request competía por ese monitor en cada línea
 * de log, y el monitor no cubría solo la redacción: al ser el
 * {@code ConsoleAppender} anidado un {@code OutputStreamAppender} —que codifica
 * <em>fuera</em> de su propio {@code streamWriteLock}, por diseño— la
 * serialización se tragaba también el encoding JSON, que es la parte cara. El
 * cuello de botella aparecía justo en el peor momento: en una tormenta de
 * errores, con la cadena de causas y varias líneas por request, el incidente
 * amplificaba su propia latencia.
 *
 * <p>
 * Quitar la exclusión mutua es seguro porque este appender <b>no tiene estado
 * mutable propio</b>: su único campo es {@link #nested}, un
 * {@link AppenderAttachableImpl} cuya lista interna es una {@code COWArrayList}
 * —la misma que usa {@code Logger} para repartir cada evento entre sus
 * appenders, en concurrencia y sin lock—. El motor de redacción
 * ({@link LogRedactor}) es estático y sin estado: sus {@code Pattern} son
 * inmutables y todo {@code Matcher} y {@code StringBuilder} es local a la
 * llamada. {@link RedactedLoggingEvent} y {@link RedactedThrowable} se
 * construyen por evento, y cada evento está confinado al hilo que lo emitió.
 *
 * <p>
 * <b>Lo que se conserva del comportamiento anterior:</b> el {@code guard} de
 * {@code AppenderBase} —que descartaba las invocaciones reentrantes— se
 * sustituye por un {@code ReentryGuard} de tipo {@code THREAD_LOCAL} en
 * {@link #buildReentryGuard()}, porque el guard por defecto de
 * {@code UnsynchronizedAppenderBase} es {@code NOP}. Es la misma decisión que
 * toma {@code ConsoleAppender}, y en un appender por el que pasa todo el log
 * del proceso no conviene dejar abierta la vía a una recursión: cuesta un
 * {@code ThreadLocal} por hilo, sin memoria compartida ni contención.
 *
 * @see LogRedactor
 * @see RedactedLoggingEvent
 */
public final class RedactingAppender extends UnsynchronizedAppenderBase<ILoggingEvent>
        implements
            AppenderAttachable<ILoggingEvent> {

    private final AppenderAttachableImpl<ILoggingEvent> nested = new AppenderAttachableImpl<>();

    @Override
    public void start() {
        if (!nested.iteratorForAppenders().hasNext()) {
            addWarn("No hay appenders anidados en [" + name + "]; sus eventos se descartan.");
        }
        super.start();
    }

    /**
     * Protección contra reentrada por hilo, equivalente a la que daba el
     * {@code guard} de {@code AppenderBase} y sin el monitor que lo acompañaba. El
     * valor por defecto de {@link UnsynchronizedAppenderBase} es {@code NOP}.
     */
    @Override
    protected ReentryGuard buildReentryGuard() {
        return ReentryGuardFactory.makeGuard(ReentryGuardFactory.GuardType.THREAD_LOCAL);
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
