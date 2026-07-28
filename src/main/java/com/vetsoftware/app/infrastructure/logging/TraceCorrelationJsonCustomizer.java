package com.vetsoftware.app.infrastructure.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.boot.json.JsonWriter;
import org.springframework.boot.logging.structured.StructuredLoggingJsonMembersCustomizer;

/**
 * Completa el JSON nativo de Spring Boot con metadatos estables y una señal que indica si el
 * evento pertenece a una traza real.
 *
 * <p>Micrometer Tracing es el único dueño de {@code traceId} y {@code spanId}: los publica en el
 * MDC durante el alcance del span y el encoder de Spring Boot los serializa. Fuera de ese alcance
 * se omiten, porque inventar identificadores impediría navegar correctamente hacia Tempo.
 */
public final class TraceCorrelationJsonCustomizer
        implements StructuredLoggingJsonMembersCustomizer<ILoggingEvent> {

    private static final Pattern TRACE_ID = Pattern.compile("[0-9a-f]{32}");
    private static final Pattern SPAN_ID = Pattern.compile("[0-9a-f]{16}");

    @Override
    public void customize(JsonWriter.Members<ILoggingEvent> members) {
        members.add("service", event -> contextProperty(event, "appName"));
        members.add("instanceId", event -> contextProperty(event, "instanceId"));
        members.add("traceCorrelated", event -> isTraceCorrelated(event.getMDCPropertyMap()));
    }

    static boolean isTraceCorrelated(Map<String, String> mdc) {
        if (mdc == null) {
            return false;
        }
        return TRACE_ID.matcher(mdc.getOrDefault("traceId", "")).matches()
                && SPAN_ID.matcher(mdc.getOrDefault("spanId", "")).matches();
    }

    private static String contextProperty(ILoggingEvent event, String name) {
        return event.getLoggerContextVO().getPropertyMap().get(name);
    }
}
