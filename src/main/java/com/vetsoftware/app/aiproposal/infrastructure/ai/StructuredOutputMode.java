package com.vetsoftware.app.aiproposal.infrastructure.ai;

import java.util.Arrays;
import java.util.Locale;

/**
 * Como se le exige al modelo que la salida sea JSON, y <strong>por que es
 * configuracion y no una rama escrita a fuego</strong>.
 *
 * <p>
 * ⛔ <strong>El uso de herramientas es una capacidad POR MODELO, no de la
 * API.</strong> {@code Converse} es la interfaz unificada de Bedrock y por eso
 * este adaptador va contra ella, pero unificar la interfaz no unifica lo que
 * cada familia sabe hacer: hay modelos del catalogo de Bedrock que no admiten
 * {@code toolConfig} en absoluto, y otros que lo admiten sin {@code strict}.
 * Atar la salida estructurada a la via mas fuerte y no dejar salida deja el
 * sistema en el peor sitio posible el dia que se cambie de familia: no se
 * degrada, <strong>se rompe entero</strong>, y hace falta un despliegue para
 * arreglarlo.
 *
 * <p>
 * Los tres valores son los tres escalones reales de esa capacidad, de mas a
 * menos acoplamiento. <strong>Bajar un escalon es una linea de configuracion,
 * no un cambio de codigo</strong>, y el prompt pide el formato por escrito en
 * los tres —cinturon y tirantes: el mecanismo puede no estar disponible, las
 * instrucciones siempre lo estan—.
 *
 * <p>
 * <strong>Lo que NO cambia entre escalones</strong> es todo lo de aguas abajo:
 * los tres producen la misma cadena en {@code ModelInvocation.rawJson}, la lee
 * el mismo parser y un cuerpo ilegible degrada por el mismo desenlace
 * declarado. La eleccion mueve donde se garantiza la forma, no que pasa cuando
 * no se cumple.
 */
public enum StructuredOutputMode {

    /**
     * Herramienta forzada <em>con</em> esquema estricto. El proveedor valida la
     * entrada de la herramienta contra el esquema —campos obligatorios, tipos, sin
     * propiedades de mas— antes de emitir el bloque, asi que la forma la garantiza
     * el, no un parrafo del prompt.
     *
     * <p>
     * Es el defecto porque es la unica opcion que <em>garantiza</em> algo. Y es la
     * mas acoplada: la admiten los modelos de la familia Anthropic en Bedrock, no
     * todo el catalogo.
     *
     * <p>
     * <strong>Si el modelo configurado no la admite, el fallo es gratis y
     * ruidoso</strong>: Bedrock rechaza la peticion con {@code ValidationException}
     * antes de inferir nada —no se factura—, eso viaja como
     * {@code MODEL_INVALID_REQUEST}, que es sistemico y se escribe con
     * {@code ERROR}, y el log dice literalmente que se pruebe {@link #TOOL} o
     * {@link #PROMPT}.
     */
    TOOL_STRICT,

    /**
     * Herramienta forzada <em>sin</em> {@code strict}. El escalon para un modelo
     * que sabe llamar herramientas pero rechaza el esquema estricto. Sigue llegando
     * el bloque de herramienta con su JSON ya estructurado; lo que se pierde es la
     * validacion del esquema en el proveedor, asi que un campo de mas o un tipo
     * raro los tiene que absorber el parser —que ya lo hace—.
     */
    TOOL,

    /**
     * Sin {@code toolConfig}: el formato se pide por escrito y se lee del bloque de
     * texto. <strong>Funciona en cualquier modelo que Bedrock sirva por
     * {@code Converse}</strong>, incluidos los que no tienen herramientas.
     *
     * <p>
     * Es el escalon sin garantia: aqui la forma la pone la buena voluntad del
     * modelo y la comprueba el parser. Que sea el menos fiable no lo hace opcional
     * —es la unica via universal— y por eso las instrucciones piden el objeto JSON
     * de forma inequivoca aunque los otros dos escalones no lo necesiten.
     */
    PROMPT;

    /**
     * ⛔ <strong>Un valor que no se reconoce revienta el arranque, y es
     * deliberado.</strong> El resto de la configuracion de esta feature usa
     * funciones totales —{@code BedrockInvokerConfig.ACTIVO} compara contra la
     * cadena {@code true} justo para no reventar—, pero alli un fallo dejaria el
     * puerto sin ningun bean y tumbaria las 93 rodajas de integracion. Aqui no:
     * aqui lo unico que hay al otro lado de un valor mal escrito es <em>elegir otro
     * mecanismo del que el operador no queria</em>. Caer al defecto en silencio es
     * exactamente el fallo que el dueno prohibio: alguien cambia el modelo, escribe
     * mal el modo, y se entera tres semanas despues mirando un panel. Un arranque
     * rojo con el valor mal escrito en el mensaje se ve en el primer despliegue.
     *
     * <p>
     * Y solo se lee cuando Bedrock esta encendido: con
     * {@code bedrock.enabled=false} esta clase no la instancia nadie, asi que un
     * dedazo aqui no puede tumbar un despliegue que ni siquiera usa el modelo.
     */
    public static StructuredOutputMode of(String valor) {
        String normalizado = valor == null ? "" : valor.trim().toUpperCase(Locale.ROOT);
        return Arrays.stream(values()).filter(modo -> modo.name().equals(normalizado)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "vetsoftware.ai.proposal.bedrock.structured-output no reconoce '" + valor
                                + "'; los valores validos son TOOL_STRICT, TOOL y PROMPT"));
    }

    /** {@code true} si hay que adjuntar {@code toolConfig} a la peticion. */
    public boolean usaHerramienta() {
        return this != PROMPT;
    }

    /** {@code true} si el esquema de la herramienta viaja con {@code strict}. */
    public boolean esEstricto() {
        return this == TOOL_STRICT;
    }
}
