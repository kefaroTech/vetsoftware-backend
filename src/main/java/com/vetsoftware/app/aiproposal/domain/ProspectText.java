package com.vetsoftware.app.aiproposal.domain;

import java.util.Objects;

/**
 * El texto libre que escribio el prospecto, envuelto en un tipo que <strong>no
 * se puede imprimir</strong>.
 *
 * <p>
 * ⛔ <strong>R1 del anexo B —"el texto libre no sale por ninguna senal"— era una
 * regla sin mecanismo.</strong> Estaba escrita, nadie la comprobaba, y la forma
 * de romperla es la mas facil que existe: alguien anade dentro de seis meses un
 * {@code span.setAttribute("prospect.text", texto)} o un
 * {@code log.info("propuesta para {}", texto)} porque esta depurando, y el
 * texto de un tercero —con el nombre de su clinica, su facturacion y a veces su
 * telefono— acaba en Loki con 31 dias de retencion. No hay revision de codigo
 * que cace eso de forma fiable, porque en el diff se lee razonable.
 *
 * <p>
 * <strong>El mecanismo es este tipo, y funciona por construccion.</strong>
 * Practicamente toda tuberia de telemetria del proyecto convierte a cadena por
 * el mismo sitio: el {@code {}} de SLF4J, los valores del MDC, los atributos de
 * un span de OpenTelemetry y cualquier {@code String.valueOf}. Todos llaman a
 * {@link #toString()}, y aqui {@link #toString()} devuelve la
 * <em>longitud</em>, que es justo la unica medida que el anexo B si autoriza a
 * emitir. Sacar el texto de verdad exige nombrar {@link #revealForModelCall()},
 * que no se escribe sin darse cuenta y salta a la vista en cualquier revision.
 *
 * <p>
 * <strong>Y tampoco se serializa.</strong> No es un {@code record} ni tiene
 * getters con forma de propiedad, asi que Jackson no encuentra nada que sacar:
 * un DTO que lo arrastre por descuido no publica el texto por HTTP.
 *
 * <p>
 * <strong>Lo que este tipo NO hace</strong>: no cifra, no sanea y no impide que
 * quien llame a {@link #revealForModelCall()} haga con el resultado lo que
 * quiera. Sube el coste de la fuga accidental de "una linea distraida" a "una
 * linea deliberada"; contra lo deliberado no protege nada.
 */
public final class ProspectText {

    /** Espejo de {@code @Size(max = 1000)} del DTO inicial. */
    private static final int MAX_CHARS = 1000;

    private final String text;

    private ProspectText(String text) {
        this.text = text;
    }

    /**
     * @throws IllegalArgumentException
     *             si el texto esta vacio o pasa de 1.000 caracteres. Los minimos
     *             (15 inicial, 10 de refinamiento) los comprueba el DTO: son
     *             politica del endpoint, no invariantes del dato.
     */
    public static ProspectText of(String text) {
        if (text == null || text.isBlank())
            throw new IllegalArgumentException("prospect text is required");
        if (text.length() > MAX_CHARS)
            throw new IllegalArgumentException(
                    "prospect text must be " + MAX_CHARS + " chars or less");
        return new ProspectText(text);
    }

    /**
     * El texto de verdad. <strong>El unico consumidor legitimo es el cuerpo de la
     * llamada al modelo</strong> y la columna {@code input_text} del turno, que se
     * anonimiza a los 90 dias. El nombre es largo y explicito a proposito.
     */
    public String revealForModelCall() {
        return text;
    }

    /** Lo unico que el anexo B autoriza a emitir sobre el texto del prospecto. */
    public int length() {
        return text.length();
    }

    /**
     * <strong>Nunca el texto.</strong> Cambiar esto por {@code return text} es
     * abrir R1 de par en par en cada log, cada MDC y cada span del backend a la
     * vez; {@code ProspectTextTest} lo fija para que el cambio rompa el build en
     * vez de una auditoria dentro de un ano.
     */
    @Override
    public String toString() {
        return "ProspectText[" + text.length() + " chars]";
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof ProspectText otro && text.equals(otro.text);
    }

    @Override
    public int hashCode() {
        return Objects.hash(text);
    }
}
