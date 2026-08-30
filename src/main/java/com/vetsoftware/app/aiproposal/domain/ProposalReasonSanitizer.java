package com.vetsoftware.app.aiproposal.domain;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Las nueve reglas de S6.4.1, en el dominio y sin Spring.
 *
 * <p>
 * ⛔ <strong>Corre EN EL SERVIDOR, antes de persistir y antes de
 * serializar.</strong> El anexo A lo situaba "en el seam" del front, y eso deja
 * fuera al correo de S6.4.2 —que es un segundo renderizador de la misma prosa—
 * y a cualquier consumidor futuro. El front conserva las mismas comprobaciones
 * como cinturon, no como control.
 *
 * <p>
 * ⛔ <strong>La regla 3 rechaza CUALQUIER digito, no las rachas largas.</strong>
 * Es el hueco caro y el motivo de que esta clase exista: el borrador anterior
 * solo cazaba rachas de cuatro o mas, asi que dejaba pasar {@code 900},
 * {@code 12} y {@code 19 %}. Y el prompt <em>obliga</em> al modelo a citar al
 * cliente, que escribe "facturamos 40 millones al mes". Ese motivo, pintado
 * bajo un {@code $46.000} que sale de {@code catalog_prices}, es una segunda
 * cifra sin lista de precios detras en la pantalla que decide una compra — y la
 * que un humano lee primero no es necesariamente la nuestra. Un motivo no
 * necesita ninguna cifra: el ejemplo bueno del anexo E no lleva ni una.
 *
 * <p>
 * <strong>El orden de evaluacion es contrato.</strong> El truncado de la regla
 * 2 se aplica <em>al final</em>, cuando las otras ocho ya dieron el visto bueno
 * sobre el texto <em>completo</em>. Truncar primero podria cortar justo el
 * trozo con la cifra y dejar pasar lo que la regla 3 existe para parar.
 *
 * <p>
 * <strong>Ser agresivo sale gratis porque el fallback es bueno.</strong>
 * {@code short_description} ya esta en espanol revisado, ya viaja en
 * {@code PublicCatalogItemDto.description} y ya es lo que pinta el catalogo
 * manual. Descartar un motivo valido cuesta cero —el cliente lee una
 * descripcion correcta en vez de una frase personalizada—; publicar uno raro,
 * no. <strong>Ante la duda, se descarta.</strong>
 */
public final class ProposalReasonSanitizer {

    private static final int MIN_CHARS = 10;

    private static final int MAX_CHARS = 140;

    /** Cuantas lineas del mismo turno pueden repetir la misma frase (regla 9). */
    private static final int MAX_REPETICIONES = 3;

    private static final Pattern CIFRA = Pattern.compile("[0-9]");

    /**
     * Dinero escrito con letras. {@code mil}, {@code peso} y {@code millon} van con
     * frontera de palabra <strong>obligatoriamente</strong>: sin ella,
     * <em>familia</em> contiene "mil" y <em>espeso</em> contiene "peso", y el
     * saneador rechazaria media prosa legitima hasta que alguien lo desactivara
     * entero, que es como muere una regla de seguridad.
     */
    private static final Pattern DINERO = Pattern.compile(
            "[$%€]|\\bCOP\\b|\\bUSD\\b|\\bpesos?\\b|\\bmillon(es)?\\b|\\bmillón(es)?\\b|\\bmil\\b",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    private static final Pattern MARCADO = Pattern.compile("[<>]|&#|javascript:|data:",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern ENLACE = Pattern.compile(
            "https?://|www\\.|\\b[a-z0-9-]+\\.(co|com|net|org|io)\\b", Pattern.CASE_INSENSITIVE);

    /**
     * Un codigo del catalogo en mayusculas. {@code [A-Z]} y no {@code \p{Lu}} a
     * proposito: los codigos son ASCII y una palabra acentuada en mayusculas no es
     * un codigo.
     */
    private static final Pattern CODIGO = Pattern.compile("\\b[A-Z][A-Z0-9_]{2,}\\b");

    /**
     * Correo o telefono. La racha de siete digitos <strong>no puede disparar
     * sola</strong> mientras la regla 3 rechace cualquier digito, y esta escrita
     * igualmente: es la mitad de la regla que sobrevive si algun dia se relaja la
     * 3, y borrarla ahora seria dejar el hueco preparado.
     */
    private static final Pattern CONTACTO = Pattern.compile("@|[0-9]{7,}");

    private ProposalReasonSanitizer() {
    }

    /**
     * Las ocho reglas que solo miran una linea. La novena necesita el turno entero
     * y vive en {@link #sanitizeTurn(Map, Map)}.
     *
     * @param reason
     *            la prosa cruda del modelo
     * @param fallback
     *            el {@code short_description} del articulo. Si falta —un articulo
     *            sin descripcion—, un motivo rechazado se queda en
     *            {@link ProposalCart#MOTIVO_AUSENTE}, que es texto fijo y no hace
     *            eco de nada
     */
    public static SanitizedReason sanitize(String reason, String fallback) {
        String limpio = reason == null ? "" : reason.trim();
        String determinista = fallback == null || fallback.isBlank()
                ? ProposalCart.MOTIVO_AUSENTE
                : fallback.trim();

        if (limpio.length() < MIN_CHARS)
            return SanitizedReason.sustituido(determinista, ReasonRejection.R1_CORTO);
        if (CIFRA.matcher(limpio).find())
            return SanitizedReason.sustituido(determinista, ReasonRejection.R3_CIFRA);
        if (DINERO.matcher(limpio).find())
            return SanitizedReason.sustituido(determinista, ReasonRejection.R4_DINERO);
        if (MARCADO.matcher(limpio).find())
            return SanitizedReason.sustituido(determinista, ReasonRejection.R5_MARCADO);
        if (ENLACE.matcher(limpio).find())
            return SanitizedReason.sustituido(determinista, ReasonRejection.R6_ENLACE);
        if (CODIGO.matcher(limpio).find())
            return SanitizedReason.sustituido(determinista, ReasonRejection.R7_CODIGO);
        if (CONTACTO.matcher(limpio).find())
            return SanitizedReason.sustituido(determinista, ReasonRejection.R8_CONTACTO);

        return limpio.length() > MAX_CHARS
                ? SanitizedReason.truncado(truncar(limpio))
                : SanitizedReason.intacto(limpio);
    }

    /**
     * El turno entero, con la regla 9 encima. Se cuenta sobre el texto
     * <strong>crudo y normalizado</strong> —no sobre el ya saneado—: si se contara
     * despues, ocho lineas sustituidas por ocho {@code short_description}
     * <em>distintos</em> no repetirian nada y el atasco del modelo quedaria
     * invisible, que es lo unico que la regla existe para ver.
     *
     * @param reasons
     *            motivo crudo por codigo de articulo
     * @param fallbacks
     *            {@code short_description} por codigo de articulo
     */
    public static Map<String, SanitizedReason> sanitizeTurn(Map<String, String> reasons,
            Map<String, String> fallbacks) {
        Map<String, String> crudos = reasons == null ? Map.of() : reasons;
        Map<String, String> deterministas = fallbacks == null ? Map.of() : fallbacks;

        Map<String, Integer> repeticiones = new HashMap<>();
        crudos.values().stream().filter(java.util.Objects::nonNull)
                .map(ProposalReasonSanitizer::clave)
                .forEach(clave -> repeticiones.merge(clave, 1, Integer::sum));

        Map<String, SanitizedReason> saneados = new LinkedHashMap<>();
        crudos.forEach((code, crudo) -> {
            String determinista = deterministas.get(code);
            if (crudo != null && repeticiones.getOrDefault(clave(crudo), 0) > MAX_REPETICIONES) {
                saneados.put(code,
                        SanitizedReason.sustituido(determinista == null || determinista.isBlank()
                                ? ProposalCart.MOTIVO_AUSENTE
                                : determinista.trim(), ReasonRejection.R9_REPETIDO));
                return;
            }
            saneados.put(code, sanitize(crudo, determinista));
        });
        return saneados;
    }

    /**
     * "El mismo texto exacto" con la holgura minima que lo hace util: el modelo
     * atascado repite la frase cambiando el espaciado o la caja, y una comparacion
     * byte a byte lo dejaria pasar.
     */
    private static String clave(String texto) {
        return texto.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    /**
     * Corta en el ultimo espacio dentro del limite y anade puntos suspensivos, para
     * no partir una palabra por la mitad. El limite cuenta ya el caracter que se
     * anade: lo que sale nunca pasa de {@value #MAX_CHARS}.
     */
    private static String truncar(String texto) {
        String recorte = texto.substring(0, MAX_CHARS - 1);
        int ultimoEspacio = recorte.lastIndexOf(' ');
        if (ultimoEspacio > 0)
            recorte = recorte.substring(0, ultimoEspacio);
        return recorte.stripTrailing() + "…";
    }
}
