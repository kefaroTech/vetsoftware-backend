package com.vetsoftware.app.infrastructure.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.pattern.ThrowableProxyConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import org.slf4j.event.KeyValuePair;
import org.springframework.boot.json.JsonWriter;
import org.springframework.boot.logging.structured.JsonWriterStructuredLogFormatter;

/**
 * Formato JSON de una línea por evento para la salida de consola de dev y prod.
 *
 * <p>
 * <b>Por qué existe:</b> en ECS lo que se escribe a stdout es lo que llega a
 * CloudWatch, y CloudWatch pasa a ser la fuente durable de logs (la cola en
 * memoria del exportador OTLP descarta en silencio cuando se llena). Mientras
 * la consola fue texto plano, esa ruta solo llevaba el mensaje y el
 * {@code traceId}: migrar a CloudWatch habría costado todos los atributos
 * estructurados que sí viajan por OTLP. Este formato emite el MDC completo, así
 * que las dos rutas transportan lo mismo.
 *
 * <p>
 * <b>Compatibilidad con la redacción (lo que hace que este cambio sea seguro):
 * </b> el formateo se hace exclusivamente desde
 * {@link ILoggingEvent#getFormattedMessage()} y
 * {@link ILoggingEvent#getThrowableProxy()}, nunca desde
 * {@code getArgumentArray()}. Eso es un requisito, no una casualidad:
 * {@link RedactedLoggingEvent} devuelve {@code null} en
 * {@code getArgumentArray()} justamente para que un encoder que reformatee no
 * pueda recomponer el mensaje <em>sin redactar</em> a partir de la plantilla y
 * sus argumentos. Un formato que leyera los argumentos reintroduciría el
 * secreto que {@link LogRedactor} acaba de quitar.
 *
 * <p>
 * <b>Por qué un formato propio y no {@code ecs}/{@code logstash}:</b> los
 * formatos comunes de Spring Boot vuelcan el MDC con su nombre original, y
 * Micrometer Tracing lo escribe como {@code traceId}. El datasource de Loki
 * enlaza a Tempo con un <em>derived field</em> cuyo patrón es
 * {@code [tT]race_?[iI][dD]"?[:=]"?(\w+)}, y el resto de la plataforma
 * (atributos OTLP, convenciones de OTel) usa {@code trace_id}. Emitirlo con ese
 * nombre exacto mantiene la correlación log→traza sin configurar nada nuevo y
 * deja las dos rutas nombrando igual el mismo dato.
 *
 * <p>
 * Los campos no declarados aquí —todo el MDC y los {@code KeyValuePair} de
 * {@code addKeyValue(...)}— se aplanan en la raíz del objeto tal como llegan
 * del evento ya redactado, que es donde {@link LogFieldPolicy} ya aplicó su
 * allowlist.
 *
 * @see RedactingAppender
 * @see RedactedLoggingEvent
 * @see LogFieldPolicy
 */
public final class StructuredConsoleLogFormatter
        extends
            JsonWriterStructuredLogFormatter<ILoggingEvent> {

    /** Clave con la que Micrometer Tracing publica la traza en el MDC. */
    private static final String MDC_TRACE_ID = "traceId";

    /** Clave con la que Micrometer Tracing publica el span en el MDC. */
    private static final String MDC_SPAN_ID = "spanId";

    /**
     * Claves que se emiten renombradas ({@code trace_id}/{@code span_id}) y por
     * tanto no se vuelven a volcar con su nombre original: duplicarlas solo añade
     * ruido a cada línea.
     */
    private static final Set<String> RENAMED_KEYS = Set.of(MDC_TRACE_ID, MDC_SPAN_ID);

    /** Instante en UTC ({@code 2026-08-18T12:34:56.789Z}). */
    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ISO_INSTANT;

    /**
     * Construido por reflexión desde {@code logback-spring.xml}: el
     * {@code StructuredLogEncoder} de Spring Boot resuelve el nombre de esta clase
     * y le inyecta los parámetros disponibles.
     *
     * @param throwableProxyConverter
     *            conversor de Logback, ya iniciado por el encoder, con el que se
     *            renderiza el stacktrace
     */
    public StructuredConsoleLogFormatter(ThrowableProxyConverter throwableProxyConverter) {
        super(members -> jsonMembers(throwableProxyConverter, members), null);
    }

    private static void jsonMembers(ThrowableProxyConverter throwableProxyConverter,
            JsonWriter.Members<ILoggingEvent> members) {
        members.add("timestamp", ILoggingEvent::getInstant).as(TIMESTAMP::format);
        members.add("level", ILoggingEvent::getLevel).as(Level::toString);
        members.add("logger", ILoggingEvent::getLoggerName);
        members.add("thread", ILoggingEvent::getThreadName);
        // getFormattedMessage(), nunca getArgumentArray(): ver el javadoc de la clase.
        members.add("message", ILoggingEvent::getFormattedMessage);
        members.add("trace_id", event -> mdcValue(event, MDC_TRACE_ID)).whenHasLength();
        members.add("span_id", event -> mdcValue(event, MDC_SPAN_ID)).whenHasLength();
        members.add().<String, String>usingPairs(StructuredConsoleLogFormatter::structuredFields);

        Function<ILoggingEvent, Object> throwableProxy = event -> (event != null)
                ? event.getThrowableProxy()
                : null;
        members.add().whenNotNull(throwableProxy)
                .usingMembers(event -> event.add("exception").usingMembers(exception -> {
                    exception.add("type", ILoggingEvent::getThrowableProxy)
                            .as(IThrowableProxy::getClassName);
                    exception.add("message", ILoggingEvent::getThrowableProxy)
                            .as(IThrowableProxy::getMessage);
                    exception.add("stacktrace", throwableProxyConverter::convert);
                }));
    }

    private static String mdcValue(ILoggingEvent event, String key) {
        Map<String, String> mdc = event.getMDCPropertyMap();
        return mdc == null ? null : mdc.get(key);
    }

    /**
     * Aplana en la raíz el MDC completo y los pares de {@code addKeyValue(...)}.
     *
     * <p>
     * Se emite todo lo que traiga el evento —el appender de OpenTelemetry captura
     * {@code captureMdcAttributes: *} y las dos rutas deben llevar lo mismo—. Los
     * valores llegan ya filtrados por {@link LogFieldPolicy}: lo que no está en la
     * allowlist vale {@link LogRedactor#MASK} desde antes de entrar aquí.
     */
    private static void structuredFields(ILoggingEvent event, BiConsumer<String, String> fields) {
        Map<String, String> mdc = event.getMDCPropertyMap();
        if (mdc != null) {
            mdc.forEach((key, value) -> {
                if (key != null && !RENAMED_KEYS.contains(key)) {
                    fields.accept(key, value);
                }
            });
        }
        List<KeyValuePair> pairs = event.getKeyValuePairs();
        if (pairs == null) {
            return;
        }
        for (KeyValuePair pair : pairs) {
            if (pair != null && pair.key != null) {
                fields.accept(pair.key, pair.value == null ? null : pair.value.toString());
            }
        }
    }
}
