package com.vetsoftware.app.infrastructure.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.LoggerContextVO;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Marker;
import org.slf4j.event.KeyValuePair;

/**
 * Vista redactada de un evento de log. Delega todo en el evento original salvo las cuatro
 * superficies por las que puede escapar un dato sensible: mensaje, MDC, pares clave-valor y la
 * cadena de excepciones.
 *
 * <p>Se sustituye el evento en lugar de intentar mutarlo porque {@code LoggingEvent} es
 * efectivamente inmutable en lo que importa, y porque así el appender destino recibe exactamente lo
 * que se va a emitir, sin poder recomponer el mensaje desde los argumentos originales:
 * {@link #getMessage()} devuelve ya el texto formateado y redactado y {@link #getArgumentArray()}
 * devuelve {@code null}. Eso es deliberado — un encoder que reformatee obtiene el mismo texto
 * redactado, y no una versión limpia reconstruida a partir de los argumentos.
 *
 * <p>Redactar sobre el mensaje <em>ya formateado</em> (y no argumento a argumento) es lo que permite
 * detectar secretos que solo existen al unir plantilla y argumento: en
 * {@code log.info("password={}", secret)} el argumento aislado es un texto anodino; el mensaje
 * formateado es {@code password=hunter2}.
 *
 * @see RedactingAppender
 * @see LogRedactor
 */
final class RedactedLoggingEvent implements ILoggingEvent {

    private final ILoggingEvent delegate;
    private final String message;
    private final Map<String, String> mdc;
    private final List<KeyValuePair> keyValuePairs;
    private final IThrowableProxy throwableProxy;

    private RedactedLoggingEvent(ILoggingEvent delegate, String message, Map<String, String> mdc,
                                 List<KeyValuePair> keyValuePairs, IThrowableProxy throwableProxy) {
        this.delegate = delegate;
        this.message = message;
        this.mdc = mdc;
        this.keyValuePairs = keyValuePairs;
        this.throwableProxy = throwableProxy;
    }

    /**
     * Redacta el evento, devolviendo el <b>original</b> si no hubo nada que cambiar. El caso normal
     * es que no haya cambios, así que no se paga ninguna copia.
     */
    static ILoggingEvent of(ILoggingEvent event) {
        String originalMessage = event.getFormattedMessage();
        String redactedMessage = LogRedactor.redact(originalMessage);

        Map<String, String> originalMdc = event.getMDCPropertyMap();
        Map<String, String> redactedMdc = LogRedactor.redactMdc(originalMdc);

        List<KeyValuePair> originalPairs = event.getKeyValuePairs();
        List<KeyValuePair> redactedPairs = LogRedactor.redactKeyValuePairs(originalPairs);

        IThrowableProxy originalThrowable = event.getThrowableProxy();
        IThrowableProxy redactedThrowable = RedactedThrowable.redact(originalThrowable);

        boolean unchanged = Objects.equals(originalMessage, redactedMessage)
                && redactedMdc == originalMdc
                && redactedPairs == originalPairs
                && redactedThrowable == originalThrowable;
        if (unchanged) {
            return event;
        }
        return new RedactedLoggingEvent(
                event, redactedMessage, redactedMdc, redactedPairs, redactedThrowable);
    }

    // --- superficies redactadas ---

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public String getFormattedMessage() {
        return message;
    }

    /** {@code null} a propósito: el mensaje ya viene formateado y redactado. */
    @Override
    public Object[] getArgumentArray() {
        return null;
    }

    @Override
    public Map<String, String> getMDCPropertyMap() {
        return mdc;
    }

    @Override
    @SuppressWarnings("deprecation")
    public Map<String, String> getMdc() {
        return mdc;
    }

    @Override
    public List<KeyValuePair> getKeyValuePairs() {
        return keyValuePairs;
    }

    @Override
    public IThrowableProxy getThrowableProxy() {
        return throwableProxy;
    }

    // --- delegación pura ---

    @Override
    public String getThreadName() {
        return delegate.getThreadName();
    }

    @Override
    public Level getLevel() {
        return delegate.getLevel();
    }

    @Override
    public String getLoggerName() {
        return delegate.getLoggerName();
    }

    @Override
    public LoggerContextVO getLoggerContextVO() {
        return delegate.getLoggerContextVO();
    }

    @Override
    public StackTraceElement[] getCallerData() {
        return delegate.getCallerData();
    }

    @Override
    public boolean hasCallerData() {
        return delegate.hasCallerData();
    }

    @Override
    public List<Marker> getMarkerList() {
        return delegate.getMarkerList();
    }

    @Override
    public long getTimeStamp() {
        return delegate.getTimeStamp();
    }

    @Override
    public int getNanoseconds() {
        return delegate.getNanoseconds();
    }

    @Override
    public Instant getInstant() {
        return delegate.getInstant();
    }

    @Override
    public long getSequenceNumber() {
        return delegate.getSequenceNumber();
    }

    @Override
    public void prepareForDeferredProcessing() {
        delegate.prepareForDeferredProcessing();
    }
}
