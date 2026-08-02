package com.vetsoftware.app.infrastructure.logging;

import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.ThrowableProxy;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Objects;
import java.util.Set;

/**
 * Excepción sintética que sustituye a una original cuyo mensaje contenía datos sensibles.
 *
 * <p>El mensaje de una excepción es un vector de fuga real y difícil de auditar: un
 * {@code DataIntegrityViolationException} arrastra el SQL con sus parámetros, y un error de cliente
 * HTTP puede arrastrar la cabecera {@code Authorization} que se envió.
 *
 * <p><b>Por qué se clona la excepción en vez de envolver el {@code IThrowableProxy}:</b> el appender
 * de OpenTelemetry solo reconoce la clase concreta {@link ThrowableProxy} y obtiene el
 * {@link Throwable} real de ella; un {@code IThrowableProxy} propio haría que OTel <em>descartara la
 * excepción por completo</em>, perdiendo tipo, mensaje y stacktrace en Loki. Así que se construye un
 * {@code Throwable} de verdad con el mensaje ya redactado y se envuelve en un {@code ThrowableProxy}
 * legítimo.
 *
 * <p><b>Fidelidad:</b> se conservan el stacktrace, la cadena de causas y las excepciones suprimidas.
 * El {@code toString()} sobrescrito devuelve el nombre de la clase original, así que la consola y el
 * atributo {@code exception.stacktrace} siguen mostrando el tipo real; solo el atributo
 * {@code exception.type} de OTel pasa a ser esta clase. Ese coste se paga <b>únicamente</b> cuando
 * la redacción cambió algo: si el mensaje estaba limpio, {@link #redact(IThrowableProxy)} devuelve el
 * proxy original sin tocarlo, que es el caso normal.
 *
 * @see LogRedactor
 */
final class RedactedThrowable extends Throwable {

    private static final long serialVersionUID = 1L;

    private final String originalType;

    private RedactedThrowable(String originalType, String message, StackTraceElement[] stackTrace,
                              Throwable cause) {
        super(message, cause, true, true);
        this.originalType = originalType;
        setStackTrace(stackTrace);
    }

    /**
     * Redacta la cadena de excepciones de un proxy de Logback.
     *
     * @param proxy proxy del evento; puede ser {@code null}
     * @return el mismo proxy si no había nada que redactar, o uno nuevo sobre la copia redactada
     */
    static IThrowableProxy redact(IThrowableProxy proxy) {
        if (!(proxy instanceof ThrowableProxy throwableProxy)) {
            // Solo ThrowableProxy expone el Throwable original. Cualquier otra implementación
            // (p.ej. un proxy deserializado) no se da en proceso; se deja pasar sin tocar.
            return proxy;
        }
        Throwable original = throwableProxy.getThrowable();
        if (original == null) {
            return proxy;
        }
        Throwable redacted = redactChain(original, Collections.newSetFromMap(new IdentityHashMap<>()));
        return redacted == original ? proxy : new ThrowableProxy(redacted);
    }

    /** Recorre causa y suprimidas de abajo arriba; el {@code visited} corta cadenas cíclicas. */
    private static Throwable redactChain(Throwable throwable, Set<Throwable> visited) {
        if (throwable == null || !visited.add(throwable)) {
            return throwable;
        }
        Throwable cause = redactChain(throwable.getCause(), visited);

        Throwable[] suppressed = throwable.getSuppressed();
        Throwable[] redactedSuppressed = new Throwable[suppressed.length];
        boolean suppressedChanged = false;
        for (int i = 0; i < suppressed.length; i++) {
            redactedSuppressed[i] = redactChain(suppressed[i], visited);
            suppressedChanged |= redactedSuppressed[i] != suppressed[i];
        }

        String message = throwable.getMessage();
        String redactedMessage = LogRedactor.redact(message);
        if (Objects.equals(message, redactedMessage)
                && cause == throwable.getCause()
                && !suppressedChanged) {
            return throwable;
        }
        RedactedThrowable copy = new RedactedThrowable(
                throwable.getClass().getName(), redactedMessage, throwable.getStackTrace(), cause);
        for (Throwable each : redactedSuppressed) {
            copy.addSuppressed(each);
        }
        return copy;
    }

    /**
     * Devuelve la primera línea con el tipo original. Logback compara este {@code toString()} con la
     * forma nominal {@code clase: mensaje} y, al diferir, lo usa como {@code overridingMessage} al
     * imprimir — así el tipo real sobrevive en la consola y en el stacktrace exportado.
     */
    @Override
    public String toString() {
        String message = getLocalizedMessage();
        return message == null ? originalType : originalType + ": " + message;
    }
}
